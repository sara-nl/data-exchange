from django.db.models import Q

from rest_framework import viewsets, serializers, status
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import IsAuthenticated, AllowAny
from backend.scripts.ResearchdriveClient import ResearchdriveClient


class ViewShares(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def create(self, request):
        rd_client = ResearchdriveClient()
        return Response({"output": rd_client.get_shares()})

    @action(
        detail=True,
        methods=["DELETE"],
        name="remove",
        permission_classes=[IsAuthenticated],
    )
    def remove(self, request, pk=None):
        rd_client = ResearchdriveClient()
        if rd_client.remove_share_by_id(rd_client.get_share_id(pk)):
            return Response(status=status.HTTP_200_OK)
        else:
            return Response(status=status.HTTP_404_NOT_FOUND)
