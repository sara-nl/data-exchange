import docker
import time
from os import path

IMAGE = "python"
COMMAND = "python /tmp/code/good-code.py"
SCRIPT_DIR = path.dirname(path.realpath(__file__))

client = docker.from_env()


def containerStatus(): return client.containers.get(container.id).status


container = client.containers.run(
    IMAGE, COMMAND, detach=True, network_disabled=True,
    volumes={
        SCRIPT_DIR + '/../input': {'bind': '/tmp/input', 'mode': 'ro'},
        SCRIPT_DIR + '/../code': {'bind': '/tmp/code', 'mode': 'ro'},
        SCRIPT_DIR + '/../output': {'bind': '/tmp/output', 'mode': 'rw'},
    })

print("! Created container {}".format(container.id))

while(containerStatus() != 'exited'):
    time.sleep(1)

print(container.logs())

container.remove()
