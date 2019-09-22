import os
import sys
import tempfile
import subprocess
import argparse

from rd_connector import download_file


class RunEnvironment:
    def __init__(self, algorithm_file_name, data_file_name, download_dir=''):
        self.algorithm_file_name = algorithm_file_name
        self.data_file_name = data_file_name

        self.download_dir = download_dir

    def download_and_run(self, username, password):
        """
            Combines downloaden and running of algorithm with data
        """
        self.download_files(username, password)
        self.run_algorithm()

    def run_algorithm(self):
        """
            Run algorithm with the data
        """
        try:
            proc = subprocess.Popen(
                [f"python3 {self.temp_algorithm_file} {self.temp_data_file}"],
                shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

            # Collect output and write to file
            self.collect_ouput(proc)

            print(
                f"Output file can be found in {os.path.join(os.getcwd(), self.download_dir)}")

            # Delete algorithm and data files
            self.remove_files()
        except KeyboardInterrupt or Exception as e:
            self.stop_running(error=e)

    def download_files(self, username, password):
        """
            Downloads both the algorithm and data

            TODO change to better download function
        """

        try:
            self.create_files()
            self.download_from_rd(username, password)

            # change permissions of files
            # algorithm_file:  read and execute
            # data_file: read
            os.chmod(self.temp_algorithm_file, 0o555)
            os.chmod(self.temp_data_file, 0o444)
        except KeyboardInterrupt or Exception as e:
            self.stop_running(error=e)

    def create_files(self):
        """
            Creates temporary data and algorithm files with unique names
        """

        if not os.path.exists(self.download_dir):
            os.mkdir(self.download_dir)

        self.temp_algorithm_file = tempfile.NamedTemporaryFile(
            suffix=".py", dir=self.download_dir, delete=False).name

        self.temp_data_file = tempfile.NamedTemporaryFile(
            suffix=".data", dir=self.download_dir, delete=False).name

    def download_from_rd(self, username, password):
        """
            Downloads both the algorithm and data from researchdrive
        """

        options = {
            'webdav_hostname': "https://researchdrive.surfsara.nl",
            'webdav_root': '/remote.php/nonshib-webdav/',
            'webdav_login': username,
            'webdav_password': password
        }

        if self.algorithm_file_name and self.data_file_name:
            try:
                download_file(options, filename=self.algorithm_file_name,
                              filepath=self.temp_algorithm_file)

                download_file(options, filename=self.data_file_name,
                              filepath=self.temp_data_file)
            except FileNotFoundError as e:
                self.stop_running(e)
        else:
            print("Need algorithm and data")

    def collect_ouput(self, proc):
        """
            Collects all output from stdout and stderr
        """

        output_file = open(f"{self.download_dir}/output.txt", 'w')

        output = f"Standard out:\n{proc.stdout.read().decode('utf-8')}"
        output += f"Standard error:\n{proc.stderr.read().decode('utf-8')}"

        output_file.write(output)

        print("\nProgram Output:")
        print(output)

    def stop_running(self, error=''):
        """
            Called if program stops runnings, deletes downloaded files
        """

        self.remove_files()
        print(f"Error occured {error}\nfiles are removed")
        sys.exit(0)

    def remove_files(self):
        """
            Removes downloaded files
        """

        if self.temp_algorithm_file:
            os.remove(self.temp_algorithm_file)

        if self.temp_data_file:
            os.remove(self.temp_data_file)


if __name__ == "__main__":
    """
        Analyses the used flags and arguments and starts the environment accordingly
    """

    parser = argparse.ArgumentParser(
        description='Arguments to connect to resource drive')
    parser.add_argument('-algorithm_file', type=str,
                        help='File with the algorithm', default='test_algorithm.py')
    parser.add_argument('-data_file', type=str,
                        help='File with data', default='test_data.txt')
    parser.add_argument('-username', type=str,
                        help='Username of Research Drive account', default='tijs@wearebit.com')
    parser.add_argument('-password', type=str,
                        help='Password of Research Drive account', default='prototypingfutures')
    parser.add_argument('-download_dir', type=str,
                        help='Directory where downloaded folders are store', default='files_con01')

    args = parser.parse_args()

    run_env = RunEnvironment(
        args.algorithm_file, args.data_file, args.download_dir)

    # run_env.download_files(args.username, args.password)
    # run_env.run_algorithm()
    run_env.download_and_run(args.username, args.password)
