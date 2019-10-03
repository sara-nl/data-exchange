from rest_framework import routers, serializers, viewsets
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.response import Response

from backend.scripts.ResearchdriveClient import ResearchdriveClient

import json


class GetUserFiles(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def list(self, request):
        own_algorithms = []
        own_datasets = []
        available_datasets = []


        rd_client = ResearchdriveClient()
        shares = rd_client.get_shares()

        for share in shares:
            if share.get("item_type") == "folder":
                continue

            filename = share.get("file_target").strip("/")

            if filename[-2:] == "py":
                if share.get("uid_owner") == str(request.user):
                    own_algorithms.append(filename)
            else:
                if share.get("uid_owner") == str(request.user):
                    own_datasets.append(filename)

                available_datasets.append(filename)

        return Response({"output": {
            "own_algorithms": own_algorithms,
            "own_datasets": own_datasets,
            "available_datasets": available_datasets}
        })
