import pika
import os
from django.core.management.base import BaseCommand, CommandError
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase

from surfsara.models import Task
from surfsara.services import mail_service


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class TaskCompleted:
    task_id: str
    state: str
    output: str


class Command(BaseCommand):
    help = "Starts listening for finished task"

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

        queue_name = "tasker_done"

        def callback(ch, method, properties, body):
            # This is also the place, where we may want to trigger the output approval
            # flow, like sending an email to the reviewer and so on. Right now I just
            # store task as completed in DB

            # Probably it needs to be wrapped in try/except too :-)
            task_completed = TaskCompleted.from_json(body)
            self.stdout.write(f"Received {task_completed}")

            task = Task.objects.get(pk=task_completed.task_id)
            task.output = task_completed.output

            if not task.review_output and task_completed.state == "success":
                task.state = Task.OUTPUT_RELEASED
            else:
                task.state = task_completed.state
            task.save()

            # TODO: Actually show the URL in the email. Currently, we can't really know
            # what domain we're hosting on. Should probably get this from an environment
            # variable, configured in the docker-compose.yml (or Django's settings.py)
            mail_service.send_mail(
                mail_files="finished_running",
                receiver=task.approver_email,
                subject="Task output is ready for approval",
            )

            self.stdout.write(f"Successfully updated task {task_completed.task_id}")

        self.channel.queue_declare(queue=queue_name)

        self.channel.basic_consume(
            queue=queue_name, on_message_callback=callback, auto_ack=True
        )

        self.channel.start_consuming()
