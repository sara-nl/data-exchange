import os
import pika
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class StartContainer:
    task_id: str
    data_path: str
    code_path: str


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class ProcessesAlgorithm:
    task_id: str
    alg_name: str


connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host=os.environ.get("RABBITMQ_HOST", "localhost"),
        credentials=pika.PlainCredentials(
            username=os.environ.get("RABBITMQ_USERNAME", "guest"),
            password=os.environ.get("RABBITMQ_PASSWORD", "guest"),
        ),
    )
)
channel = connection.channel()
channel.queue_declare(queue='algorithm_process_queue', durable=True)

properties = pika.BasicProperties(content_type="application/json", delivery_mode=1)


def callback(ch, method, properties, body):
    print(" [x] Received %r" % body)
    print(body)
    print(" [x] Done")
    ch.basic_ack(delivery_tag=method.delivery_tag)


channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue='algorithm_process_queue', on_message_callback=callback)

channel.start_consuming()