import requests
from requests.exceptions import HTTPError
import webdav3.client as wc
import lxml.etree as etree
import os
import json


class ResearchdriveClient:

    webdav_hostname = "https://researchdrive.surfsara.nl"

    share_api_endpoint = (
        webdav_hostname + "/ocs/v1.php/apps/files_sharing/api/v1/shares"
    )
    current_version_endpoint = webdav_hostname + "/remote.php/dav/files/"
    version_api_startendpoint = webdav_hostname + "/remote.php/dav/meta/"
    version_api_endendpoint = "/v"

    def __init__(self):
        self.options = {
            "webdav_hostname": ResearchdriveClient.webdav_hostname,
            "webdav_root": "/remote.php/nonshib-webdav/",
            "webdav_login": "f_data_exchange",
            "webdav_password": "KCVNI-VBXWR-NLGMO-POQNO",
        }

        self.client = None
        self.shares = {}
        self.connect()

    def connect(self):
        """
        Connect the using the options credentials.
        """
        self.client = wc.Client(self.options)

    def set_options(self, options):
        """
        Set different options credentials and reconnect the client
        :param options: Dictionary containing webdav_hostname,
        webdav_root, webdav_login and webdav_password.
        """
        self.options = options
        self.connect()

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
        remote_path = remote_path.replace(os.sep, "/")

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

    # Code below this point doens't make use of the webdav3.client package
    def get_shares(self, uid_owner=None):
        """
        Get all shared files and folder. If a uid_owner is given
        it only returns shares this owner.
        :param uid_owner: Usually an email address, represents a unique id.
        :return: A list containing a dictionary with metadata for each
        share.
        """

        params = (("shared_with_me", "true"), ("format", "json"))

        content = self.__execute_request(
            ResearchdriveClient.share_api_endpoint, "GET", params=params
        )
        shares = json.loads(content)
        self.shares = shares["ocs"]["data"]

        if uid_owner:
            self.filter_shares(uid_owner)
        return self.shares

    def filter_shares(self, uid_owner):
        """
        Filters and updates shares based on a unique id of the owner.
        :param uid_owner: Usually an email address, represents a unique id.
        """
        filtered = []
        for share in self.shares:
            if share["uid_owner"] == uid_owner:
                filtered.append(share)
        self.shares = filtered

    def get_file_versions(self, file_id, remote_path):
        """
        Gets  href, last_modified and etag of all file versions.
        :param file_id: Id of the file.
        :param remote_path: Path on the server.
        :return: List containing information aboout all file versions
        structured in dicts.
        """
        endpoint = (
            ResearchdriveClient.version_api_startendpoint
            + str(file_id)
            + ResearchdriveClient.version_api_endendpoint
        )

        current_version = self.get_current_file_version(remote_path)

        old_versions_content = self.__execute_request(
            endpoint, "PROPFIND", {"Accept": "*/*"}
        )

        combined = current_version + self.parse_version_xml(old_versions_content)

        return combined

    def get_current_file_version(self, remote_path):
        """
        Gets href, last_modified and etag, of the most recent file version.
        :param remote_path: Path on the server.
        :return: Returns a list containing a dict with the information.
        """
        endpoint = (
            ResearchdriveClient.current_version_endpoint
            + self.options["webdav_login"]
            + "/"
            + remote_path
        )

        content = self.__execute_request(
            endpoint, "PROPFIND", {"Accept": "*/*", "Depth": "1"}
        )

        return self.parse_version_xml(content)

    def __execute_request(self, endpoint, method, headers=None, params=None):
        """
        Executing request using the given variables.
        :param endpoint: Endpoint to which to request to.
        :param method: GET, PROPFIND, POST or any other method.
        :param headers: Optional - Add headers to the request
        :param params: Optional - Add extra parameters.
        :return: Returns the text response if successful.
        """
        try:
            response = requests.request(
                method=method,
                url=endpoint,
                auth=(self.options["webdav_login"], self.options["webdav_password"]),
                headers=headers,
                params=params,
            )
            response.raise_for_status()
        except HTTPError as http_error:
            print(f"An HTTP error occured: {http_error}")
            raise http_error
        except Exception as error:
            print(f"Error: {error}")
            raise error
        else:
            return response.text

    def get_file_id(self, remote_path):
        return

    def get_remote_path(self, file_id):
        return

    @staticmethod
    def parse_version_xml(content):
        """
        Parses the XML response into a list containing dictionaries.
        :param content: String of xml content.
        :return: List containing dicts with href, last_modified and etag.
        """
        tree = etree.fromstring(content)
        tree_responses = tree.findall("{DAV:}response")

        file_versions = []
        for response in tree_responses:
            version = {
                "href": response.findtext("{DAV:}href"),
                "last_modified": response.findtext(
                    "{DAV:}propstat/{DAV:}prop/{DAV:}getlastmodified"
                ),
                "etag": response.findtext("{DAV:}propstat/{DAV:}prop/{DAV:}getetag"),
            }
            file_versions.append(version)
        return file_versions


def main():
    return


if __name__ == "__main__":
    main()
