from rest_framework import viewsets, serializers, status
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import IsAuthenticated, AllowAny

from backend.scripts.SharesClient import SharesClient


class ViewShares(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def list(self, request):
        all_shares = SharesClient().all()
        email = str(request.user)
        user_shares = filter(lambda s: s["ownerEmail"] == email, all_shares)
        return Response(user_shares)
