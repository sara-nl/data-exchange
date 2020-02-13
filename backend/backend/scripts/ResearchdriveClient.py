import requests
from requests.exceptions import HTTPError
import webdav3.client as wc
import lxml.etree as etree
import os
import json
from surfsara import logger


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

    def download(self, remote_path, local_path, save_as=None, etag=None):
        """
        Download a file from the Researchdrive
        :param save_as:
        :param remote_path: Path from file or folder located on researchdrive
        :param local_path: Download save location
        :param etag: of file
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

        # Validate etag
        # if self.__is_recent_etag(remote_path, etag):
        if not etag:
            error = self.client.download_sync(remote_path, local_path)
            if not error:
                return True
            return error
        else:
            self.download_old_version(remote_path, local_path, etag)
        return False

    # Code below this point doesn't make use of the webdav3.client package
    @staticmethod
    def __compare_tag(tag, versions, tagname):
        """
        Compares tag value in a list of dicts.
        :param tag: Value you want to compare
        :param versions: A list with dictionaries.
        :param tagname: Key you want to check.
        :return: True if tag is in, false if not.
        """
        for version in versions:
            if str(version[tagname]) == str(tag):
                return version
        return False

    def download_old_version(self, remote_path, local_path, old_id, etag=None):
        """
        Download older versions of the file based of an old_id or etag.
        :param remote_path: Path on the server of the original file
        :param local_path: Location where to save the file
        :param old_id: id thats behind a version link ../1232190/v/99999
        (99999, in this case).
        :param etag: Compare etags
        :return: Download the old version file to the local_path. True
        if successful
        """

        # Get old version list
        old_versions = self.get_file_versions(remote_path)

        # Compare etags - Currently impossible because etag chances.
        if etag:
            version = self.__compare_tag(etag, old_versions, "etag")
        else:
            version = self.__compare_tag(old_id, old_versions, "old_id")

        if version:
            download_url = ResearchdriveClient.webdav_hostname + version["href"]

            content = self.__execute_request(download_url, "GET")

            try:
                with open(local_path, "w") as local_file:
                    local_file.write(content)
                return True
            except Exception as error:
                raise error
        else:
            raise KeyError("The Etag cannot be found in previous versions.")

    def get_shares(self, uid_owner=None, filename=None):
        """
        Get all shared files and folder. If a uid_owner is given
        it only returns shares this owner.
        :param uid_owner: Optional. Usually an email address, represents a unique id.
        :param filename: Optional. Filter a certain filename.
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
            self.__filter_shares_owner(uid_owner)
        if filename:
            self.__filter_shares_filename(filename)
        return self.shares

    def remove_share_by_id(self, share_id):
        endpoint = f"{ResearchdriveClient.share_api_endpoint}/{share_id}"
        response = self.__execute_request(endpoint, "DELETE")
        logger.debug(f"Remove share response {response}")
        return self.parse_revoke_share_xml(response) == "100"

    def remove_share(self, remote_path):
        shares = self.get_shares()
        share = self.__compare_tag(remote_path, shares, "file_target")
        if share:
            return self.remove_share_by_id(share["id"])
        return True

    def __filter_shares_owner(self, owner):
        """
        Filters and updates shares based on a unique id of the owner.
        :param owner: Usually an email address, represents a unique id.
        """
        self.shares = [share for share in self.shares if share["uid_owner"] == owner]

    def __filter_shares_filename(self, filename):
        self.shares = [
            share for share in self.shares if share["path"] == "/" + filename
        ]

    def __is_recent_etag(self, remote_path, etag):
        """
        Checks if the etag is from the most recent file.
        """
        if str(self.get_fileid_etag(remote_path)["etag"]) == str(etag):
            return True
        return False

    def get_fileid_etag(self, remote_path):
        """
        Retrieves file_id and etag from a remote_path.
        :param remote_path: Path on the server.
        :return: Dictionary with fileid and etag.
        """

        remote_path = remote_path.replace(os.sep, "/")

        # The payload.
        xml_content = (
            '<?xml version="1.0"?><a:propfind xmlns:a="DAV:"'
            + ' xmlns:oc="http://owncloud.org/ns"><a:prop>'
            + "<oc:fileid/><a:getetag/></a:prop></a:propfind>"
        )

        # Execute request
        url = (
            ResearchdriveClient.current_version_endpoint
            + self.options["webdav_login"]
            + "/"
            + remote_path
        )
        content = self.__execute_request(
            url, "PROPFIND", {"Content-Type": "text/xml"}, data=xml_content
        )

        return self.parse_fileid_etag_xml(content)

    def get_remote_path(self, file_id):
        shares = self.get_shares()
        for share in shares:
            if str(share["item_source"]) == str(file_id):
                return share["path"]
        return False

    def get_file_versions(self, remote_path):
        """
        Gets  href, last_modified and etag of all file versions.
        :param remote_path: Path on the server
        :return: List containing information aboout all file versions
        structured in dicts.
        """
        file_id = self.get_fileid_etag(remote_path)["file_id"]

        endpoint = (
            ResearchdriveClient.version_api_startendpoint
            + str(file_id)
            + ResearchdriveClient.version_api_endendpoint
        )

        old_versions_content = self.__execute_request(
            endpoint, "PROPFIND", {"Accept": "*/*"}
        )

        return self.parse_version_xml(old_versions_content)

    def __execute_request(self, endpoint, method, headers=None, params=None, data=None):
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
                data=data,
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
                "etag": str(
                    response.findtext("{DAV:}propstat/{DAV:}prop/{DAV:}getetag")
                ).strip('"'),
                "old_id": response.findtext("{DAV:}href").split("/")[-1],
            }
            file_versions.append(version)
        return file_versions

    @staticmethod
    def parse_fileid_etag_xml(content):
        """
        Parses a fileid and etag response into a dictionary
        :param content: xml response.
        :return: Dictionary with etag and file_id
        """
        tree = etree.fromstring(content)

        # A list containing one response
        tree_response = tree.findall("{DAV:}response")[0]
        return {
            "etag": tree_response.findtext(
                "{DAV:}propstat/{DAV:}prop/{DAV:}getetag"
            ).strip('"'),
            "file_id": tree_response.findtext(
                "{DAV:}propstat/{DAV:}prop/{http://owncloud.org/ns}fileid"
            ),
        }

    @staticmethod
    def parse_revoke_share_xml(content):
        tree = etree.fromstring(content)
        return tree.findtext(".//statuscode")


def main():
    return


if __name__ == "__main__":
    main()
