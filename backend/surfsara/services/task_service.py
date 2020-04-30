import os
import pika
from surfsara.models import Task
from surfsara.messages import StartContainer, AnalyzeArtifact
from surfsara.management.commands.listen import AnalyzeListener


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
        task_id=str(task.id),
        data_path=task.dataset,
        code_path=task.algorithm,
        code_hash={"eTag": task.permission.algorithm_etag},
    )

    channel.basic_publish(
        exchange="tasker_todo",
        routing_key="tasker_todo",
        body=command.to_json(),
        properties=PROPERTIES,
    )

    connection.close()


def analyze(permission_id: str):
    connection, channel = __connect()
    command = AnalyzeArtifact(permission_id)

    channel.basic_publish(
        exchange="",
        routing_key=AnalyzeListener.queue_name,
        body=command.to_json(),
        properties=PROPERTIES,
    )
    connection.close()
