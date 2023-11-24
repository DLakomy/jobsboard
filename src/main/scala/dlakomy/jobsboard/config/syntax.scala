package dlakomy.jobsboard.config

import cats.effect.*
import cats.syntax.all.*
import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag


object syntax:
  extension (source: ConfigSource)
    def loadF[F[_], A: ClassTag: ConfigReader](using F: Concurrent[F]): F[A] =
      F.pure(source.load[A])
        .flatMap:
          case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
          case Right(value) => F.pure(value)
