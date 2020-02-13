from django.db.models import Q

from rest_framework import viewsets, serializers, status
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import IsAuthenticated, AllowAny
from backend.scripts.ResearchdriveClient import ResearchdriveClient

import json
from functools import reduce

from backend.scripts.ResearchdriveClient import ResearchdriveClient
from surfsara.services.files_service import OwnShares


class ViewShares(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    rd_client = ResearchdriveClient()

    def list(self, request):
        def as_name_id(share):
            return {
                "name": share.get("file_target").strip("/"),
                "id": share.get("id"),
                "isDirectory": share.get("item_type") == "folder",
            }

        alg_shares, data_shares = OwnShares(str(request.user)).return_own_shares()

        return Response(
            {
                "own_algorithms": map(as_name_id, alg_shares),
                "own_datasets": map(as_name_id, data_shares),
            }
        )

    @action(
        detail=True,
        methods=["DELETE"],
        name="remove",
        permission_classes=[IsAuthenticated],
    )
    def remove(self, request, pk=None):
        if self.rd_client.remove_share_by_id(pk):
            return Response(status=status.HTTP_200_OK)
        else:
            return Response(status=status.HTTP_404_NOT_FOUND)
