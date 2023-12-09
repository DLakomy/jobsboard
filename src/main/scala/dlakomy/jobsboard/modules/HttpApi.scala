package dlakomy.jobsboard.modules

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import dlakomy.jobsboard.config.*
import dlakomy.jobsboard.core.Users
import dlakomy.jobsboard.domain.security
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.http.routes.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.BackingStore
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.authentication.SecuredRequestHandler
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256


class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]):
  given SecuredHandler[F]  = SecuredRequestHandler(authenticator)
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F](core.jobs).routes
  private val authRoutes   = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router("/api" -> (healthRoutes <+> jobRoutes <+> authRoutes))


object HttpApi:

  def createAuthenticator[F[_]: Sync](users: Users[F], securityConfig: SecurityConfig): F[Authenticator[F]] =
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
    yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration,
      maxIdle = None,
      identityStore = idStore,
      tokenStore = tokenStore,
      signingKey = key
    )

  def apply[F[_]: Async: Logger](core: Core[F], securityConfig: SecurityConfig): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map: authenticator =>
        new HttpApi[F](core, authenticator)
