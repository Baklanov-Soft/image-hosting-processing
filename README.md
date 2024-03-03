# Image hosting processing

Support part of https://github.com/Baklanov-Soft/image-hosting-storage

## Resizer

Resizer service for generating the previews. Docker Compose contains 2 instances by default (=partitions amount of
new images topic).

Environment variables:

```
KAFKA_BOOTSTRAP_SERVERS - kafka cluster url (Default: localhost:9092)
CONSUMER_GROUP_ID - consumer id, multiple instances with same id will allow horizontal scaling (depends on topic paritions) (Default: resizer-local-test)
NEW_IMAGES_TOPIC - topic for notifications about new images (Default: "new-images.v1")
MINIO_HOST - host of minio from where it will take pictures and where it is going to upload the previews
MINIO_USER
MINIO_PASSWORD
```

### Protocol

Resizer reads `{NEW_IMAGES_TOPIC}` Kafka topic and accepts messages in following format (v1):

```json
{
  "bucketId": "00000000-0000-0000-0000-000000000000",
  "imageId": "a10a119a-bbc1-4980-9c23-ac28da4fe773"
}
```

It creates multiple preview images inside the same Minio as it reads from (inside subfolders in buckets).

## Recognizer

Service for object detection and nsfw content detection.

NSFW detection based on model: https://huggingface.co/Falconsai/nsfw_image_detection

Converted to DJL TorchScript model (required for service to
work): https://huggingface.co/DenisNovac/nsfw_image_detection/tree/main

Environment variables:

```
KAFKA_BOOTSTRAP_SERVERS - kafka cluster url (Default: localhost:9092)
CONSUMER_GROUP_ID - consumer id, multiple instances with same id will allow horizontal scaling (depends on topic paritions) (Default: recognizer-local-test)
NEW_IMAGES_TOPIC - topic for notifications about new images (Default: "new-images.v1")
CATEGORIES_TOPIC - topic for output of service (Default: "categories.v1")
DEBUG_CATEGORIES - write debug object detection pictures (draw squares around detected objects) into debug folder (HEAVY PNG)
NSFW_SYNSET - synset.txt file for nsfw detector (list of categories, included in project)
NSFW_MODEL_PATH - pre-trained model for nsfw detection, requires one specific model, others could be working wrong
MINIO_HOST - host of minio from where it will take pictures
MINIO_USER
MINIO_PASSWORD
```

### Protocol

Recognizer reads `{NEW_IMAGES_TOPIC}` Kafka topic and accepts messages in following format (v1):

```json
{
  "bucketId": "00000000-0000-0000-0000-000000000000",
  "imageId": "a10a119a-bbc1-4980-9c23-ac28da4fe773"
}
```

Recognizer writes output to `{CATEGORIES_TOPIC}` Kafka topic in following format (v1):

```json
{
  "bucketId": "00000000-0000-0000-0000-000000000000",
  "imageId": "9c2b08f3-66ae-4cba-b0f7-857fc96eb6b3",
  "categories": {
    "nsfw": 0.9998799562454224,
    "person": 0.7969253063201904
  }
}
```

It might write the debug picture into the debug folder of minio if `{DEBUG_CATEGORIES}` flag is true such as:

![alt text](debug_example.png)
