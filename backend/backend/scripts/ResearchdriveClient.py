import requests
from requests.exceptions import HTTPError
import webdav3.client as wc
import os
import json


class ResearchdriveClient:

    share_api_hostname = "https://researchdrive.surfsara.nl/ocs/v1.php/" \
                         "apps/files_sharing/api/v1/shares"

    def __init__(self):
        self.options = {"webdav_hostname": "https://researchdrive.surfsara.nl",
                        "webdav_root": "/remote.php/nonshib-webdav/",
                        "webdav_login": "f_data_exchange",
                        "webdav_password": "KCVNI-VBXWR-NLGMO-POQNO"}
        self.client = wc.Client(self.options)
        self.shares = {}

    def list(self, remote_path=""):
        """
        :param remote_path: Optional, if you only want to list a specific
        folder.
        :return: List of files
        """
        return self.client.list(remote_path)

    def download(self, remote_path, local_path, save_as=None):
        """
        Download a file from the Researchdrive
        :param save_as:
        :param remote_path: Path from file or folder located on researchdrive
        :param local_path: Download save location
        :return: True if successful, the error if not.
        """
        # The WebDav endpoint only supports forward slash.
        remote_path = remote_path.replace(os.sep, '/')

        if save_as:
            filename = save_as
        else:
            # If no is given a file or folder name has to be added to the local path.
            filename = [name for name in remote_path.split("/")][-1]

        local_path = os.path.join(local_path, filename)

        # Folder has to be created if it doesn't exist, otherwise webdavclient
        # randomly deletes folders.
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
        """
        Get all shared files and folder. If a uid_owner is given
        it only returns shares this owner.
        :param uid_owner: Usually an email address, represents a unique id.
        :return: A list containing a dictionary with metadata for each
        share.
        """
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
                self.filter_shares(uid_owner)
            return self.shares

    def filter_shares(self, uid_owner):
        """
        Filters and updates shares based on a unique id.
        :param uid_owner: Usually an email address, represents a unique id.
        """
        filtered = []
        for share in self.shares:
            if share["uid_owner"] == uid_owner:
                filtered.append(share)
        self.shares = filtered


def main():
    return


if __name__ == "__main__":
    main()
