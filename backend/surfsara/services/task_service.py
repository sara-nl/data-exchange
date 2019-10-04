from surfsara.models import Task
import pika
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class StartContainer:
    task_id: int
    data_path: str
    code_path: str


def start(task: Task):
    # TODO: Set up some way of pooling connections instead of
    #       opening a new one every time.
    connection = pika.BlockingConnection()
    channel = connection.channel()

    properties = pika.BasicProperties(content_type="application/json", delivery_mode=1)
    command = StartContainer(
        task_id=task.id, data_path=task.dataset, code_path=task.algorithm
    )

    channel.basic_publish(
        exchange="tasker_todo",
        routing_key="tasker_todo",
        body=command.to_json(),
        properties=properties,
    )

    connection.close()
