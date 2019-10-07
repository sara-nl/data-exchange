from surfsara.models import Task
import pika
from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase

from backend.scripts.run_container import RunContainer


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class StartContainer:
    task_id: str
    data_path: str
    code_path: str


def start(task: Task):
    # TODO: Set up some way of pooling connections instead of
    #       opening a new one every time.
    connection = pika.BlockingConnection()
    channel = connection.channel()

    properties = pika.BasicProperties(content_type="application/json", delivery_mode=1)
    command = StartContainer(
        task_id=str(task.id), data_path=task.dataset, code_path=task.algorithm
    )

    channel.basic_publish(
        exchange="tasker_todo",
        routing_key="tasker_todo",
        body=command.to_json(),
        properties=properties,
    )

    connection.close()


# def start_container(algorithm, dataset):
#     runner = RunContainer(
#         remote_algorithm_path=algorithm,
#         remote_data_path=dataset,
#         download_dir="./files",
#     )

#     try:
#         runner.download_files()
#         file = runner.run_algorithm()
#         with open(file, "r") as f:
#             output = f.read()
#     except Exception as error:
#         print(error)
#         output = "Could not run with selected files.\nPlease refresh and try again."

#     return output
