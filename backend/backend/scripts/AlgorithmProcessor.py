import os
from ResearchdriveClient import ResearchdriveClient
from multiprocessing import Process
import string
import tempfile

class AlgorithmProcessor:

    def __init__(self, algorithm_name, data_requester):
        self.algorithm_name = algorithm_name
        self.data_requester = data_requester
        self.rd_client = ResearchdriveClient()
        self.is_folder = self.is_folder()
        self.processes = []
        self.files = []

    def create_temp_file(self):
        return

    def update_file_rights(self):
        return

    def is_folder(self):
        """
        A folder can look like a file by giving the folder a name like folder.py.
        This functions checks the recorded filetype as saved on researchdrive.
        :return: True or False if it's a folder.
        """
        shares = self.rd_client.get_shares(self.data_requester, self.algorithm_name)

        if "directory" in shares[0]['mimetype']:
            return True
        return False

    def download_file(self):
        tempname = tempfile.NamedTemporaryFile(dir=os.getcwd(), delete=False).name
        print(tempname)
        print(files)
        rd_client.download(self.algorithm_name + "/" + files[0], os.getcwd(), tempname)
        return

    def update_database(self):
        return

    def process(self):
        if self.is_folder:
            for file in self.rd_client.list(self.algorithm_name):
                remote_path = self.algorithm_name + "/" + file
                algorithm_process = Process(target=self.process_algorithm, args=(remote_path,))
                algorithm_process.start()
                self.processes.append(algorithm_process)

            for process in self.processes:
                process.join()

            print("finished")

        else:
            self.process_algorithm(self.algorithm_name)

        return

    def process_algorithm(self, remote_path):

        # Download to temp file.
        tempname = tempfile.NamedTemporaryFile(dir=os.getcwd(), delete=False).name
        self.rd_client.download(remote_path, os.getcwd(), tempname)

        original_name = remote_path.split("/")[-1]

        # Open file, analyze and save information
        with open(os.path.join(os.getcwd(), tempname), "r") as algorithm_file:
            lines = algorithm_file.readlines()
            algorithm_content = " ".join(line for line in lines)
            algorithm_info = self.calculate_algorithm_info(lines)

        self.files.append({"algorithm_name": original_name, "algorithm_content": algorithm_content,
                           "algorithm_info": algorithm_info})

        print(self.files)
        # Cleanup
        os.remove(tempname)



        # Save information to Task
        # download_container = RunContainer(algorithm, "", download_dir=os.getcwd())
        # download_container.create_files()
        # download_container.download_from_rd(data=False)
        # algorithm_content = None
        # algorithm_info = None
        #
        # if download_container.temp_algorithm_file:
        #     with open(download_container.temp_algorithm_file, "r") as algorithm_file:
        #         lines = algorithm_file.readlines()
        #         algorithm_content = " ".join(line for line in lines)
        #         algorithm_info = Tasks.calculate_algorithm_info(lines)
        #
        # download_container.remove_files()
        # task.algorithm_content = algorithm_content
        # task.algorithm_info = algorithm_info
        # task.save()
        return

    @staticmethod
    def calculate_algorithm_info(lines):
        imports = []
        characters = 0
        newline = 0
        words = 0

        for line in lines:

            # Retrieve import packages.
            if "from" in line:
                imports.append(line.split("from ")[1].split(" ")[0])
            elif "import" in line:
                imports.append(line.split("import ")[1].split(" ")[0].strip("\n"))

            # Number of newlines.
            if "\n" in line:
                newline += 1
            characters += len([char for char in line])

            # Calculate words with removed punctuation and newline.
            stripped_newline = "".join(
                [char for char in line if char not in string.punctuation]
            ).strip("\n")
            words += len(
                [word for word in stripped_newline.split(" ") if len(word) > 1]
            )
        return (
                f"{characters} chars, {newline} line breaks, {words} words. "
                + f'Packages: {", ".join(imports)}'
        )


def main():
    a = AlgorithmProcessor("INT_30", "sander@wearebit.com")
    a.process()

    return


if __name__ == "__main__":
    main()
