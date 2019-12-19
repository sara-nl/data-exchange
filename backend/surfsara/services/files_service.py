from backend.scripts.SharesClient import SharesClient
from functools import reduce


class OwnShares:
    def __init__(self, user):
        shares_client = SharesClient()
        own_shares_with_metadata = list(
            filter(lambda s: s["share"]["uid_owner"] == user, shares_client.all())
        )

        (self.alg_shares, self.data_shares) = reduce(
            self.reducer, own_shares_with_metadata, ([], [])
        )

    def return_own_shares(self):
        return self.alg_shares, self.data_shares

    def reducer(self, acc, share_with_metadata):
        if share_with_metadata["isAlgorithm"]:
            return (acc[0] + [share_with_metadata["share"]], acc[1])
        else:
            return (acc[0], acc[1] + [share_with_metadata["share"]])
