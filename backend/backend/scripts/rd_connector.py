import argparse
import requests
import webdav3.client as wc
import os
import json

import tempfile


def parse_arguments():
    parser = argparse.ArgumentParser(
        description="Arguments to connect to resource drive"
    )
    parser.add_argument(
        "-username", type=str, help="Username of Research Drive account", default=False
    )
    parser.add_argument(
        "-password", type=str, help="Password of Research Drive account", default=False
    )
    parser.add_argument(
        "-download_file", type=str, help="Filename you want to download", default=False
    )
    parser.add_argument(
        "-list_files", type=bool, help="Indexes the files in a list", default=False
    )
    parser.add_argument(
        "-token",
        type=str,
        help="Token to login instead of username and password",
        default=False,
    )

    args = parser.parse_args()
    return args


class ResearchdriveClient:

    # RD THIRD "f_data_exchange" "KCVNI-VBXWR-NLGMO-POQNO"
    options = {"webdav_hostname": "https://researchdrive.surfsara.nl",
               "webdav_root": "/remote.php/nonshib-webdav/",
               "webdav_login": "tijs@wearebit.com",
               "webdav_password": "prototypingfutures"}

    share_api_hostname = "https://researchdrive.surfsara.nl/ocs/v1.php/" \
                         "apps/files_sharing/api/v1/shares"

    def __init__(self):
        self.client = wc.Client(ResearchdriveClient.options)
        self.shares = {}

    def list(self, remote_path=""):
        """
        :param remote_path: Optional, if you only want to list a specific
        folder.
        :return: List of files
        """
        return self.client.list(remote_path)

    def download(self, remote_path, local_path):
        """
        Download a file from the Researchdrive
        :param remote_path: Path from file or folder located on researchdrive
        :param local_path: Download save location
        :return: True if successful, the error if not.
        """
        # The WebDav endpoint only supports forward slash.
        remote_path = remote_path.replace(os.sep, '/')

        # We have to add file or folder name to the local path.
        filename = [name for name in remote_path.split("/")][-1]
        local_path = os.path.join(local_path, filename)

        # Folder has to be created if it doesn't exist, otherwise webdavclient
        # shows strange bugs.
        if self.client.is_dir(remote_path):
            try:
                os.mkdir(local_path)
            except FileExistsError:
                pass

        error = self.client.download_sync(remote_path, local_path)
        if not error:
            return True
        return error

    def get_shares(self, uid_owner=None):
        params = (
            ("shared_with_me", "true"),
            ("format", "json"),
        )

        response = requests.get(ResearchdriveClient.share_api_hostname,
                                params=params,
                                auth=(ResearchdriveClient.options["webdav_login"],
                                      ResearchdriveClient.options["webdav_password"]))

        shares = json.loads(response.text)
        self.shares = shares['ocs']['data']

        if uid_owner:
            self.filter_owner_uid(uid_owner)
        return self.shares

    def filter_owner_uid(self, uid_owner):
        filtered = []
        for share in self.shares:
            if share["uid_owner"] == uid_owner:
                filtered.append(share)
        self.shares = filtered


def list_files(options):
    client = wc.Client(options)
    return client.list()


def download_file(options, filename, filepath=""):
    """
        Downloads file from research drive and store is in temporary file
    """

    client = wc.Client(options)

    download_location = os.path.join(os.getcwd(), filepath)

    if client.check(filename):
        client.download_sync(remote_path=filename, local_path=download_location)
        print(f"File succesfully downloaded to: {download_location}")

    else:
        print("Could not locate the file")
        raise FileNotFoundError


def main():
    # args = parse_arguments()
    #
    # options = {
    #     "webdav_hostname": "https://researchdrive.surfsara.nl",
    #     "webdav_root": "/remote.php/nonshib-webdav/",
    # }
    #
    # if args.token:
    #     options["token"] = args.token
    # else:
    #     options["webdav_login"] = args.username
    #     options["webdav_password"] = args.password
    #
    # if args.list_files == True:
    #     print(list_files(options))
    #
    # if args.download_file:
    #     print(download_file(options, args.download_file))
    #
    # return 0

    w = ResearchdriveClient()
    x = os.getcwd()
    y = os.path.join(os.getcwd(), "test")
    z = os.path.join("Data Exchange Project", "Test.ipynb")
    a = "test_data.txt"
    b = "Data Exchange Project"
    print(w.list("Data Exchange Project"))
    #print(w.download("DataBits", y))
    w.get_shares()

    for share in w.shares:
        print(share["id"], share['uid_owner'], share['path'])
    print("----")

    w.get_shares("sander@wearebit.com")
    for share in w.shares:
        print(share["id"], share['uid_owner'], share['path'])


if __name__ == "__main__":
    main()
