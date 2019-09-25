import requests
from requests.exceptions import HTTPError
import webdav3.client as wc
import os
import json


class ResearchdriveClient:

    share_api_hostname = "https://researchdrive.surfsara.nl/ocs/v1.php/" \
                         "apps/files_sharing/api/v1/shares"

    def __init__(self):
        # RD THIRD "f_data_exchange" "KCVNI-VBXWR-NLGMO-POQNO"
        self.options = {"webdav_hostname": "https://researchdrive.surfsara.nl",
                        "webdav_root": "/remote.php/nonshib-webdav/",
                        "webdav_login": "tijs@wearebit.com",
                        "webdav_password": "prototypingfutures"}
        self.client = wc.Client(self.options)
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

        try:
            response = requests.get(ResearchdriveClient.share_api_hostname,
                                    params=params,
                                    auth=(self.options["webdav_login"],
                                          self.options["webdav_password"]))
            response.raise_for_status()
        except HTTPError as http_error:
            print(f'An HTTP error occured: {http_error}')
        except Exception as error:
            print(f'Error: {error}')
        else:
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


def main():
    return 0
    # w = ResearchdriveClient()
    # x = os.getcwd()
    # y = os.path.join(os.getcwd(), "test")
    # z = os.path.join("Data Exchange Project", "Test.ipynb")
    # a = "test_data.txt"
    # b = "Data Exchange Project"
    # print(w.list("Data Exchange Project"))
    # #print(w.download("DataBits", y))
    # w.get_shares()
    #
    # for share in w.shares:
    #     print(share["id"], share['uid_owner'], share['path'])
    # print("----")
    #
    # w.get_shares("sander@wearebit.com")
    # for share in w.shares:
    #     print(share["id"], share['uid_owner'], share['path'])


if __name__ == "__main__":
    main()
