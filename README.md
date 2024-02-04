# Image hosting processing

Support part of https://github.com/Baklanov-Soft/image-hosting-storage

## Resizer

Resizer service for generating the previews. Docker Compose contains 2 instances by default (=partitions amount of
topic).

Environment variables:

```
KAFKA_BOOTSTRAP_SERVERS - kafka cluster url (Default: localhost:9092)
CONSUMER_GROUP_ID - consumer id, multiple instances with same id will allow horizontal scaling (depends on topic paritions) (Default: resizer-local-test)
NEW_IMAGES_TOPIC - topic for notifications about new images (Default: "new-images.v1")
```

Build manually:

```
sbt buildResizer
docker build ./resizer
```