import os
import pika
from surfsara.models import Task
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


def start(task: Task, exchange):
    # TODO: Set up some way of pooling connections instead of
    #       opening a new one every time.
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

    properties = pika.BasicProperties(content_type="application/json", delivery_mode=1)

    if exchange == "tasker_todo":
        route = "tasker_todo"
        command = StartContainer(
            task_id=str(task.id), data_path=task.dataset, code_path=task.algorithm
        )
    elif exchange == "algorithm_processed":
        exchange = "tasker_todo"
        route = "tasker_done"
        command = ProcessesAlgorithm(
            task_id=str(task.id), alg_name=task.algorithm
        )
        print("CALL dat process")
    else:
        return False

    channel.basic_publish(
        exchange=exchange,
        routing_key=route,
        body=command.to_json(),
        properties=properties,
    )

    connection.close()


def send():
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

    message = ProcessesAlgorithm(task_id='1', alg_name='TEST23222name')
    channel.basic_publish(
        exchange='tasker_todo',
        routing_key='algorithm_process_queue',
        body=message.to_json(),
        properties=pika.BasicProperties(
            delivery_mode=1,  # make message persistent
        ))
    print(" [x] Sent %r" % message)
    connection.close()
