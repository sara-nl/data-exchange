from rest_framework import routers, serializers, viewsets
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.response import Response

from backend.scripts.ResearchdriveClient import ResearchdriveClient

import json
from functools import reduce


class GetUserFiles(viewsets.ViewSet):
    permission_classes = (AllowAny,)
    folderRunScript = "run.py"
    rd_client = ResearchdriveClient()

    def list(self, request):
        def is_algorithm(share):
            if share.get("item_type") == "folder":
                return self.folderRunScript in self.rd_client.list(share.get("path"))
            else:
                return share.get("path")[-3:] == ".py"

        def can_be_dataset(share):
            return share.get("path")[-3:] != ".py"

        def as_name(share):
            return share.get("file_target").strip("/")

        def belongs_to_requester(share):
            return share.get("uid_owner") == str(request.user)

        def reducer(acc, share):
            if is_algorithm(share):
                return (acc[0] + [share], acc[1])
            elif can_be_dataset(share):
                return (acc[0], acc[1] + [share])
            else:
                return acc

        own_shares = filter(belongs_to_requester, self.rd_client.get_shares())

        (alg_shares, data_shares) = reduce(reducer, own_shares, ([], []))

        return Response(
            {
                "output": {
                    "own_algorithms": map(as_name, alg_shares),
                    "own_datasets": map(as_name, data_shares),
                }
            }
        )
