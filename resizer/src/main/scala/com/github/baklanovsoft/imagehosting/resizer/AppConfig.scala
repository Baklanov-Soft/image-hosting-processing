package com.github.baklanovsoft.imagehosting.resizer

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class AppConfig(
    kafkaBootstrapServers: String,
    consumerGroupId: String,
    newImagesTopic: String
)

object AppConfig {
  implicit val configReader: ConfigReader[AppConfig] = deriveReader
}
