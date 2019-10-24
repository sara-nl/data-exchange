import os
from ResearchdriveClient import ResearchdriveClient


class AlgorithmProcessor:

    def __init__(self, algorithm_name, data_requester):
        self.algorithm_name = algorithm_name
        self.data_requester = data_requester
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
        rd_client = ResearchdriveClient()
        shares = rd_client.get_shares(self.data_requester, self.algorithm_name)

        if "directory" in shares[0]['mimetype']:
            print(rd_client.list(self.algorithm_name))
            rd_client.
            return True
        return False

    def download_file(self):
        return

    def update_database(self):
        return

    def process(self):

        if not self.is_folder():
            self.download_file()
        else:
            # list folder
            a = 1

        return

    @staticmethod
    def process_algorithm(task, algorithm):
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
    a.is_folder()

    return


if __name__ == "__main__":
    main()
