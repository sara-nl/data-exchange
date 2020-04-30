import os
import requests

from surfsara import logger
from functools import reduce


class SharesClient:

    uri = os.getenv("SHARES_API_URL")

    def all(self):
        logger.debug(f"Requesting shares info from {self.uri}")
        response = requests.request(method="GET", url=self.uri)
        response.raise_for_status()
        logger.debug(f"Response: {response.status_code}")
        return response.json()

    def algorithms_shared_by_user(self, email: str):
        all_shares = self.all()
        return [
            s["share"]["path"]
            for s in all_shares
            if s["isAlgorithm"] and s["share"]["uid_owner"] == email
        ]

    # Fetches shares from the cache and selects only the ones
    # shared by the user with `email`.
    # Returns a tuple each member whereof contains a list of shares or None.
    # Order: algorithm shares, data shares
    def user_shares_grouped(self, email: str):
        all_shares = self.all()

        if all_shares is None:
            return None, None
        else:
            own_shares_with_metadata = filter(
                lambda s: s["share"]["uid_owner"] == email, all_shares
            )

            def reducer(acc, share_with_metadata):
                share = share_with_metadata["share"]
                share_normalized = {
                    "name": share.get("file_target").strip("/"),
                    "id": share.get("id"),
                    "isDirectory": share.get("item_type") == "folder",
                }
                if share_with_metadata["isAlgorithm"]:
                    return acc[0] + [share_normalized], acc[1]
                else:
                    return acc[0], acc[1] + [share_normalized]

            return reduce(reducer, own_shares_with_metadata, ([], []))
