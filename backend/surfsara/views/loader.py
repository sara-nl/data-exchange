from rest_framework import routers, serializers, viewsets
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.response import Response

from backend.scripts.ResearchdriveClient import ResearchdriveClient
from surfsara.services.files_service import OwnShares

import json
from functools import reduce


class GetUserFiles(viewsets.ViewSet):
    permission_classes = (AllowAny,)
    folderRunScript = "run.py"
    rd_client = ResearchdriveClient()

    def list(self, request):
        alg_shares, data_shares = OwnShares(str(request.user)).return_own_shares()

        return Response(
            {
                "output": {
                    "own_algorithms": map(as_name_id, alg_shares),
                    "own_datasets": map(as_name_id, data_shares),
                }
            }
        )
