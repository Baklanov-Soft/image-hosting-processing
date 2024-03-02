package com.github.baklanovsoft.imagehosting.kafka

import cats.implicits._
import cats.effect.kernel.{Async, Resource}
import fs2.kafka._
import fs2.Stream
import org.apache.kafka.common.TopicPartition

class KafkaConsumer[F[_]: Async, K, V](bootstrapServers: String, consumerGroup: String, topic: String)(implicit
    keyDeserializer: Resource[F, KeyDeserializer[F, K]],
    valueDeserializer: Resource[F, ValueDeserializer[F, V]]
) {

  private val consumerSettings =
    ConsumerSettings[F, K, V]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(false)
      .withAllowAutoCreateTopics(false)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(consumerGroup)

  /** Simple processor */
  def streamR: Resource[F, Stream[F, CommittableConsumerRecord[F, K, V]]] =
    KafkaConsumer
      .resource(consumerSettings)
      .evalMap { consumer =>
        consumer.subscribeTo(topic) >> Async[F].pure(consumer.records)
      }

  /** Per-partitions streams needed for transactional producer */
  def streamPerPartitionR: Resource[F, Stream[F, Map[TopicPartition, Stream[F, CommittableConsumerRecord[F, K, V]]]]] =
    KafkaConsumer
      .resource(consumerSettings)
      .evalMap(consumer => consumer.subscribeTo(topic) >> Async[F].pure(consumer.partitionsMapStream))
}
