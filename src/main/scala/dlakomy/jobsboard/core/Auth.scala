package dlakomy.jobsboard.core

import cats.effect.*
import cats.implicits.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.user.*
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt


trait Auth[F[_]]:
  def login(email: String, password: String): F[Option[User]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]


class LiveAuth[F[_]: Sync: Logger] private (users: Users[F]) extends Auth[F]:
  override def login(email: String, password: String): F[Option[User]] =
    for
      maybeUser <- users.find(email)
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
    yield maybeValidatedUser

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
  def apply[F[_]: Sync: Logger](users: Users[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users).pure[F]
