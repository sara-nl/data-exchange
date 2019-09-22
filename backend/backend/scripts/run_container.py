import os
import sys
import tempfile
import subprocess
import argparse

import time
import docker

from rd_connector import download_file


class RunContainer:
    def __init__(self, algorithm_file_name, data_file_name, download_dir=""):
        self.algorithm_file_name = algorithm_file_name
        self.data_file_name = data_file_name

        self.download_dir = download_dir

        self.container = None

    def download_and_run(self, username, password):
        """
            Combines downloaden and running of algorithm with data
        """
        self.download_files(username, password)
        self.run_algorithm()

    def run_algorithm(self):
        """
            run algorithm with the data in closed docker container
        """
        try:
            self.start_container()
            self.collect_ouput()

            self.remove_files()
            self.container.remove()

            print("=== Running Finished ===")
        except:
            self.stop_running()

    def download_files(self, username, password):
        """
            Downloads both the algorithm and data

            TODO change to better download function
        """

        try:
            print("=== Downloading files ===")

            self.create_files()
            self.download_from_rd(username, password)

            print("Changed file permissions to read-only")
            # change permissions of files
            # algorithm_file:  read and execute
            # data_file: read
            os.chmod(self.temp_algorithm_file, 0o555)
            os.chmod(self.temp_data_file, 0o444)

            print("Files are ready to use\n")

        except:
            self.stop_running()

    def start_container(self):
        """
            Starts a networkless docker container and runs algorithm with data.
            Algorithm and data are mounted read-only.
        """

        print("=== Running in container ===")

        client = docker.from_env()

        command = f"python3 -u /tmp/files/{self.temp_algorithm_name} /tmp/files/{self.temp_data_name}"
        image = "python"

        self.container = client.containers.run(
            image,
            command,
            detach=True,
            network_disabled=True,
            volumes={
                os.path.join(os.getcwd(), self.download_dir): {
                    "bind": "/tmp/files",
                    "mode": "ro",
                }
            },
        )

        print(f"Created container {format(self.container.id)}")
        print(f"Container status: {self.container.status}\n")

    def collect_ouput(self):
        """
            Collects all output from stdout and stderr.
            Firstly reads them from stream and then writes to file
        """

        print("=== Collecting container output ===")
        print("Program Output:")

        container_stream = self.container.logs(stream=True)
        for lines in container_stream:
            print(lines.decode("utf-8"), end="")

        output = self.container.logs().decode("utf-8")
        output_file = tempfile.NamedTemporaryFile(
            prefix="output_", suffix=".txt", dir=self.download_dir, delete=False
        )

        self.temp_output_file = output_file.name

        with open(self.temp_output_file, "w") as f:
            f.write(output)

        print(f"\nOutput in {self.temp_output_file}\n")

    def create_files(self):
        """
            Creates temporary data and algorithm files with unique names
        """

        if not os.path.exists(self.download_dir):
            os.mkdir(self.download_dir)

        self.temp_algorithm_file = tempfile.NamedTemporaryFile(
            suffix=".py", dir=self.download_dir, delete=False
        ).name

        self.temp_data_file = tempfile.NamedTemporaryFile(
            suffix=".data", dir=self.download_dir, delete=False
        ).name

        self.temp_algorithm_name = self.temp_algorithm_file.split("/")[-1]
        self.temp_data_name = self.temp_data_file.split("/")[-1]

    def download_from_rd(self, username, password):
        """
            Downloads both the algorithm and data from researchdrive
        """

        options = {
            "webdav_hostname": "https://researchdrive.surfsara.nl",
            "webdav_root": "/remote.php/nonshib-webdav/",
            "webdav_login": username,
            "webdav_password": password,
        }

        if self.algorithm_file_name and self.data_file_name:
            try:
                download_file(
                    options,
                    filename=self.algorithm_file_name,
                    filepath=self.temp_algorithm_file,
                )

                download_file(
                    options, filename=self.data_file_name, filepath=self.temp_data_file
                )
            except FileNotFoundError as e:
                self.stop_running(e)
        else:
            print("Need algorithm and data")

    def stop_running(self, error=""):
        """
            Called if program stops running, deletes downloaded files and stops container
        """

        self.remove_files()

        if self.container:
            self.container.remove()

        print(f"Error occured {error}\nFiles and container are removed")
        sys.exit(0)

    def remove_files(self):
        """
            Removes downloaded files.
        """

        if self.temp_algorithm_file:
            os.remove(self.temp_algorithm_file)

        if self.temp_data_file:
            os.remove(self.temp_data_file)


def containerStatus(client, container):
    return client.containers.get(container.id).status


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Arguments to connect to resource drive"
    )
    parser.add_argument(
        "-algorithm_file",
        type=str,
        help="File with the algorithm",
        default="test_algorithm.py",
    )
    parser.add_argument(
        "-data_file", type=str, help="File with data", default="test_data.txt"
    )
    parser.add_argument(
        "-username",
        type=str,
        help="Username of Research Drive account",
        default="tijs@wearebit.com",
    )
    parser.add_argument(
        "-password",
        type=str,
        help="Password of Research Drive account",
        default="prototypingfutures",
    )
    parser.add_argument(
        "-download_dir",
        type=str,
        help="Directory where downloaded folders are store",
        default="files",
    )

    args = parser.parse_args()

    run_env = RunContainer(args.algorithm_file, args.data_file, args.download_dir)

    # run_env.download_files(args.username, args.password)
    # run_env.run_algorithm()
    run_env.download_and_run(args.username, args.password)
