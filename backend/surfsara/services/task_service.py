import os
import pika
from surfsara.models import Task, Permission
from surfsara.messages import StartContainer, AnalyzeArtifact


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
        data_location={
            "storage": task.dataset_storage,
            "path": {"segments": [task.dataset]},
        },
        code_location={
            "storage": task.algorithm_storage,
            "path": {"segments": [task.algorithm]},
        },
        code_hash=task.permission.algorithm_etag if task.permission == Permission.USER_PERMISSION else None,
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

    channel.basic_publish(
        exchange="",
        routing_key="tasker_analyze",
        body=AnalyzeArtifact(permission_id).to_json(),
        properties=PROPERTIES,
    )

    connection.close()
