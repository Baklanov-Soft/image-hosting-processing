package com.github.baklanovsoft.imagehosting.imagehosting.kafka

import cats.effect.kernel.{Async, Resource}
import fs2.kafka._
import fs2.Stream

class KafkaConsumer[F[_]: Async, K, V](bootstrapServers: String, consumerGroup: String, topic: String)(implicit
    keyDeserializer: Resource[F, KeyDeserializer[F, K]],
    valueDeserializer: Resource[F, ValueDeserializer[F, V]]
) {

  private val consumerSettings =
    ConsumerSettings[F, K, V]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(consumerGroup)

  def stream: Stream[F, CommittableConsumerRecord[F, K, V]] =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(topic)
      .records
}
