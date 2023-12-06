package dlakomy.jobsboard.domain

import cats.*
import cats.syntax.all.*
import dlakomy.jobsboard.domain.user.*
import org.http4s.Response
import org.http4s.*
import tsec.authentication.*
import tsec.authorization.AuthorizationInfo
import tsec.authorization.BasicRBAC
import tsec.mac.jca.HMACSHA256


object security:
  type Crypto              = HMACSHA256
  type JwtToken            = AugmentedJWT[HMACSHA256, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type AuthRBAC[F[_]]      = BasicRBAC[F, Role, User, JwtToken]

  // RBAC
  given authRole[F[_]: MonadThrow]: AuthorizationInfo[F, Role, User] with
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations:
    given [F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance: (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)

  extension [F[_]](authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  given auth2tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] = auths =>
    val unauthorizedService: TSecAuthService[User, JwtToken, F] = TSecAuthService[User, JwtToken, F]: _ =>
      Response[F](Status.Unauthorized).pure[F]

    auths.rbacRoutes.toSeq
      .foldLeft(unauthorizedService):
        case (acc, (rbac, routes)) =>
          val bigRoute = routes.reduce(_ orElse _)
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
