package com.github.baklanovsoft.imagehosting.resizer

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class AppConfig(
    kafkaBootstrapServers: String,
    consumerGroupId: String,
    newImagesTopic: String,
    minio: MinioCreds
)

final case class MinioCreds(
    host: String,
    user: String,
    password: String
)

object MinioCreds {
  implicit val configReader: ConfigReader[MinioCreds] = deriveReader
}

object AppConfig {
  implicit val configReader: ConfigReader[AppConfig] = deriveReader
}
