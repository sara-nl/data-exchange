import os
import pika
from surfsara.models import Task
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase
from surfsara.management.commands.listen import AnalyzeListener


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class StartContainer:
    task_id: str
    data_path: str
    code_path: str
    code_hash: dict


def __connect():
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
    return connection, channel


PROPERTIES = pika.BasicProperties(content_type="application/json", delivery_mode=1)


def start(task: Task):
    connection, channel = __connect()

    command = StartContainer(
        task_id=str(task.id), data_path=task.dataset, code_path=task.algorithm,
        code_hash={"eTag": task.algorithm_etag},
    )

    channel.basic_publish(
        exchange="tasker_todo",
        routing_key="tasker_todo",
        body=command.to_json(),
        properties=PROPERTIES,
    )

    connection.close()


def analyze(task: Task):
    connection, channel = __connect()

    command = AnalyzeListener.Command(task_id=str(task.id))
    channel.basic_publish(
        exchange="",
        routing_key=AnalyzeListener.queue_name,
        body=command.to_json(),
        properties=PROPERTIES,
    )

    connection.close()
