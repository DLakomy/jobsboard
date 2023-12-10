package dlakomy.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*


final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    pass: String,
    frontendUrl: String,
    sender: String
) derives ConfigReader
