from django.db.models import Q

from rest_framework import viewsets, serializers
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import IsAuthenticated, AllowAny
from backend.scripts.ResearchdriveClient import ResearchdriveClient


class ViewShares(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        rd_client = ResearchdriveClient()
        return Response({"output": rd_client.get_shares()})

    @action(detail=True, methods=["DELETE"], name="remove", permission_classes=[AllowAny])
    def remove(self, request, pk=None):
        return Response({"pk": pk})
