package dlakomy.jobsboard.core

import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import dlakomy.jobsboard.config.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.domain.user.*
import org.typelevel.log4cats.Logger
import tsec.authentication.BackingStore
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt


trait Auth[F[_]]:
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def authenticator: Authenticator[F]
  def delete(email: String): F[Boolean]


class LiveAuth[F[_]: Async: Logger] private (users: Users[F], override val authenticator: Authenticator[F])
    extends Auth[F]:
  override def login(email: String, password: String): F[Option[JwtToken]] =
    for
      maybeUser <- users.find(email)
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
      maybeJwtToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    yield maybeJwtToken

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    users
      .find(newUserInfo.email)
      .flatMap:
        case Some(_) => None.pure[F]
        case None =>
          for
            hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
            user = User(
              newUserInfo.email,
              hashedPassword,
              newUserInfo.firstName,
              newUserInfo.lastName,
              newUserInfo.company,
              Role.RECRUITER
            )
            _ <- users.create(user)
          yield Some(user)

  override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] =

    def updateUser(user: User, newPassword: String): F[Option[User]] = for
      newHashedPassword <- BCrypt.hashpw[F](newPasswordInfo.newPassword)
      updatedUser       <- users.update(user.copy(hashedPassword = newHashedPassword))
    yield updatedUser

    def checkAndUpdate(user: User, oldPassword: String, newPassword: String): F[Either[String, Option[User]]] = for
      passCheck <- BCrypt.checkpwBool[F](newPasswordInfo.oldPassword, PasswordHash[BCrypt](user.hashedPassword))
      updateResult <-
        if (passCheck) updateUser(user, newPassword).map(Right(_))
        else Left("Invalid password").pure[F]
    yield updateResult

    users
      .find(email)
      .flatMap:
        case None => Right(None).pure[F]
        case Some(user) =>
          val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
          checkAndUpdate(user, oldPassword, newPassword)

  override def delete(email: String): F[Boolean] =
    users.delete(email)


object LiveAuth:
  def apply[F[_]: Async: Logger](users: Users[F])(securityConfig: SecurityConfig): F[LiveAuth[F]] =

    val idStoreF: F[IdentityStore[F, String, User]] = Ref
      .of[F, Map[String, User]](Map.empty)
      .map: ref =>
        new BackingStore[F, String, User]:
          override def get(email: String): OptionT[F, User] =
            val effect = for
              inMemoryUser <- ref.get.map(imm => imm.get(email))
              maybeUser    <- if (inMemoryUser.isEmpty) users.find(email) else inMemoryUser.pure[F]
              _            <- if (inMemoryUser.isEmpty) maybeUser.map(put).sequence else None.pure[F]
            yield maybeUser

            OptionT(effect)

          override def put(user: User): F[User] =
            ref.modify(imm => (imm + (user.email -> user), user))

          override def update(user: User): F[User] = put(user)

          override def delete(email: String): F[Unit] =
            ref.modify(imm => (imm - email, ()))

    val tokenStoreF = Ref
      .of[F, Map[SecureRandomId, JwtToken]](Map.empty)
      .map: ref =>
        new BackingStore[F, SecureRandomId, JwtToken]:
          override def get(id: SecureRandomId): OptionT[F, JwtToken] =
            OptionT(ref.get.map(_.get(id)))
          override def put(elem: JwtToken): F[JwtToken] =
            ref.modify(store => (store + (elem.id -> elem), elem))
          override def update(v: JwtToken): F[JwtToken] =
            put(v)
          override def delete(id: SecureRandomId): F[Unit] =
            ref.modify(store => ((store - id), ()))

    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    for
      key        <- keyF
      idStore    <- idStoreF
      tokenStore <- tokenStoreF
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = securityConfig.jwtExpiryDuration,
        maxIdle = None,
        identityStore = idStore,
        tokenStore = tokenStore,
        signingKey = key
      )
    yield new LiveAuth[F](users, authenticator)
