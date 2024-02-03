# pip3.11 uninstall kafka
# pip3.11 install kafka-python

import json
from kafka import KafkaProducer

producer = KafkaProducer(bootstrap_servers='localhost:9092')

topic = "new-images.v1"

# jsonl file
with open('messages.jsonl') as lines:
    for l in lines:
        parsed = json.loads(l)
        print(parsed)
        bytes = json.dumps(parsed).encode('utf-8')
        producer.send(topic, bytes)
    lines.close()

producer.flush()
producer.close()
