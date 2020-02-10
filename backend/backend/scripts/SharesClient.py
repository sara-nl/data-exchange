import os
import requests

from surfsara import logger


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
            if s["isAlgorithm"] == True and s["share"]["uid_owner"] == email
        ]
