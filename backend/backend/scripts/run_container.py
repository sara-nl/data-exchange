import os
import sys
import tempfile
import docker

from rd_connector import ResearchdriveClient


class RunContainer:
    def __init__(self, remote_algorithm_path, remote_data_path, download_dir=""):
        self.remote_algorithm_path = remote_algorithm_path
        self.remote_data_path = remote_data_path
        self.download_dir = download_dir
        self.container = None

        self.temp_algorithm_file = None
        self.temp_data_file = None
        self.temp_algorithm_name = None
        self.temp_data_name = None
        self.temp_output_file = None

    def download_and_run(self):
        """
            Combines downloaden and running of algorithm with data
        """
        self.download_files()
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
        except (Exception, KeyboardInterrupt, EnvironmentError) as e:
            self.stop_running()

    def download_files(self):
        """
            Downloads both the algorithm and data
        """

        try:
            print("=== Downloading files ===")

            self.create_files()
            self.download_from_rd()

            print("Changed file permissions to read-only")
            # change permissions of files
            # algorithm_file:  read and execute
            # data_file: read
            os.chmod(self.temp_algorithm_file, 0o555)
            os.chmod(self.temp_data_file, 0o444)

            print("Files are ready to use\n")

        except (Exception, KeyboardInterrupt, EnvironmentError) as e:
            self.stop_running(error=e)

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

    def download_from_rd(self):
        """
            Downloads both the algorithm and data from researchdrive
        """

        rd_client = ResearchdriveClient()

        if self.remote_algorithm_path and self.remote_data_path:
            try:
                rd_client.download(self.remote_algorithm_path, self.download_dir, self.temp_algorithm_file)
                rd_client.download(self.remote_data_path, self.download_dir, self.temp_data_file)
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


def container_status(client, container):
    return client.containers.get(container.id).status


def main():
    return


if __name__ == "__main__":
    main()