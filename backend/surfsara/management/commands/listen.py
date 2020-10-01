import pika
import os
import sys
import json
from django.core.management.base import BaseCommand, CommandError

from surfsara.models import Permission
from surfsara.models import Task
from surfsara.messages import AnalyzeArtifact
from surfsara.services import mail_service
from surfsara import logger


class Listener:
    def __init__(self, stdout, stderr, channel):

        self.stdout = stdout
        self.stderr = stderr
        self.channel = channel

    def listen(self):
        logger.info(f"Starting to listen on queue {self.queue_name}")
        self.channel.queue_declare(queue=self.queue_name)
        logger.debug(f"Declared channel")
        self.channel.basic_consume(
            queue=self.queue_name, on_message_callback=self.callback, auto_ack=True
        )
        logger.debug(f"Set up consumer")


class TaskerDoneListener(Listener):

    queue_name = "tasker_done"

    # Constants for encoding of states in incoming messages
    TASKER_STATE_REJECTED = "Rejected"
    TASKER_STATE_SUCCESS = "Success"
    TASKER_STATE_ERROR = "Success"

    TASKER_TERMINAL_STATES = [
        TASKER_STATE_REJECTED,
        TASKER_STATE_SUCCESS,
        TASKER_STATE_ERROR,
    ]

    def callback(self, ch, method, properties, body):
        logger.debug(f"New message {body}")
        task_progress = json.loads(body)
        logger.debug(f"Successfully parsed message {task_progress}")
        task = Task.objects.get(pk=int(task_progress["taskId"]))
        logger.debug(f"Updating task '{task}'")
        task.progress_state = task_progress["state"]
        if task_progress["state"]["name"] == self.TASKER_STATE_REJECTED:
            logger.debug(f"Task has been rejected")
            task.permission.state = Permission.ABORTED
            task.permission.status_description = (
                f"Revoking permission automatically: {task_progress['state']['reason']}"
            )
            task.permission.save()
            task.state = Task.ERROR
            task.save()
            return

        if "output" in task_progress["state"]:
            task.output = f"{task_progress['state']['output']['stdout']}\n{task_progress['state']['output']['stderr']}"

        if (
            not task.review_output
            and task_progress["state"]["name"] == self.TASKER_STATE_SUCCESS
        ):
            task.state = Task.OUTPUT_RELEASED
        else:
            task.state = task_progress["state"]["name"].lower()

        task.save()

        if task_progress["state"]["name"] in self.TASKER_TERMINAL_STATES:
            mail_service.send_mail(
                mail_files="finished_running",
                receiver=task.approver_email,
                subject="Task output is ready for approval",
            )

        logger.info(f"Successfully updated task {task_progress['taskId']}")


class Command(BaseCommand):
    help = "Starts listening for finished task and analysis requests"

    def __init__(self):
        super().__init__()
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=os.environ.get("RABBITMQ_HOST", "localhost"),
                credentials=pika.PlainCredentials(
                    username=os.environ.get("RABBITMQ_USERNAME", "guest"),
                    password=os.environ.get("RABBITMQ_PASSWORD", "guest"),
                ),
            )
        )
        self.channel = self.connection.channel()

    def handle(self, *args, **options):
        self.stdout.write(
            self.style.SUCCESS("Starting task results listener. To exit press CTRL+C")
        )

        TaskerDoneListener(self.stdout, self.stderr, self.channel).listen()

        try:
            self.channel.start_consuming()
        except:
            logger.exception(sys.exc_info()[0])
            self.connection.close()
