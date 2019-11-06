from backend.scripts.ResearchdriveClient import ResearchdriveClient
from functools import reduce


class OwnShares:
    folderRunScript = "run.py"
    rd_client = ResearchdriveClient()

    def __init__(self, user):
        self.user = user
        self.shares = list(
            filter(self.belongs_to_requester, self.rd_client.get_shares())
        )
        self.reduce_shares()
        self.alg_shares = []
        self.data_shares = []

    def return_own_shares(self):
        return self.alg_shares, self.data_shares

    def is_algorithm(self, share):
        if share.get("item_type") == "folder":
            return self.folderRunScript in self.rd_client.list(share.get("path"))
        else:
            return share.get("path")[-3:] == ".py"

    @staticmethod
    def can_be_dataset(share):
        return share.get("path")[-3:] != ".py"

    def belongs_to_requester(self, share):
        return share.get("uid_owner") == self.user

    def reducer(self, acc, share):
        if self.is_algorithm(share):
            return acc[0] + [share], acc[1]
        elif self.can_be_dataset(share):
            return acc[0], acc[1] + [share]
        else:
            return acc

    def reduce_shares(self):
        (self.alg_shares, self.data_shares) = reduce(
            self.reducer, self.shares, ([], [])
        )

    def get_share_name(self, id):
        for share in self.shares:

            if share["id"] == str(id):
                return share["file_target"].strip("/")

        return id
