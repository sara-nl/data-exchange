import pika
import os
from django.core.management.base import BaseCommand, CommandError
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase

from surfsara.models import Task, Permission
from surfsara.services import mail_service
from backend.scripts.AlgorithmProcessor import AlgorithmProcessor


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class TaskCompleted:
    task_id: str
    state: str
    output: str


class Listener:
    def __init__(self, stdout, stderr, channel):
        self.stdout = stdout
        self.stderr = stderr
        self.channel = channel

    def listen(self):
        self.stdout.write(f"Starting to listen on queue {self.queue_name}")
        self.channel.queue_declare(queue=self.queue_name)
        self.channel.basic_consume(
            queue=self.queue_name, on_message_callback=self.callback, auto_ack=True
        )


class TaskerDoneListener(Listener):
    @dataclass
    @dataclass_json(letter_case=LetterCase.CAMEL)
    class TaskCompleted:
        task_id: str
        state: str
        output: str

    queue_name = "tasker_done"

    def callback(self, ch, method, properties, body):
        # Probably it needs to be wrapped in try/except too :-)
        task_completed = self.TaskCompleted.from_json(body)
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


class AnalyzeListener(Listener):
    @dataclass
    @dataclass_json
    class Command:
        task_id: str

    queue_name = "analyze"

    def callback(self, ch, method, properties, body):
        command = self.Command.from_json(body)
        self.stdout.write(f"Received {command}")

        task = Task.objects.get(pk=command.task_id)
        task.save()

        processor = AlgorithmProcessor(task.algorithm, task.author_email)
        task.algorithm_content = processor.start_processing()
        task.algorithm_info = processor.calculate_algorithm_total()
        etag = processor.get_etag()
        task.algorithm_etag = etag
        task.permission.algorithm_etag = etag
        task.permission.save()
        self.stdout.write(f"Etag found: {etag}")

        # If the permission is active we shouldn't change the task state, because it
        # is not necessary for the data owner to review the algorithm.
        if task.permission.state != Permission.ACTIVE:
            task.state = Task.DATA_REQUESTED
        task.save()


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

    def listen_analyze(self):
        queue_name = "analyze"

        def callback(ch, method, properties, body):

            self.channel.queue_declare(queue=queue_name)
            self.channel.basic_consume(
                queue=queue_name, on_message_callback=callback, auto_ack=True
            )

    def handle(self, *args, **options):
        self.stdout.write(
            self.style.SUCCESS("Starting task results listener. To exit press CTRL+C")
        )

        TaskerDoneListener(self.stdout, self.stderr, self.channel).listen()
        AnalyzeListener(self.stdout, self.stderr, self.channel).listen()

        self.channel.start_consuming()
