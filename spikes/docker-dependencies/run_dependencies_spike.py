import docker
import os
import subprocess
import time

client = docker.from_env()
client.images.build(path=".", tag="new")

def containerStatus(): return client.containers.get(container.id).status

command = "python -u /tmp/code/dependencies_spike_test.py"
image = 'new:latest'

container = client.containers.run(
    image, command, detach=True, network_disabled=True,
    volumes={
        os.path.join(os.getcwd(), "code"): {'bind': '/tmp/code', 'mode': 'ro'}
    })
while(containerStatus() != 'exited'):
    time.sleep(1)

print(container.logs())

container.remove()
