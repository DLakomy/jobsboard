package dlakomy.jobsboard.fixtures

import cats.data.OptionT
import cats.effect.*
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.domain.user.*
import org.http4s.*
import org.http4s.headers.Authorization
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import concurrent.duration.*


trait SecuredRouteFixture extends UsersFixture:

  val mockedAuthenticator: Authenticator[IO] =
    val key =
      HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == dawidEmail) OptionT.pure(dawid)
      else if (email == johnEmail) OptionT.pure(john)
      else OptionT.none

    JWTAuthenticator.unbacked.inBearerToken(1.day, None, idStore, key)

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders:
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
