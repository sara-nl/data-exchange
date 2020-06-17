from rest_framework import viewsets, serializers, status
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import IsAuthenticated, AllowAny

from backend.scripts.SharesClient import SharesClient


class ViewShares(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def list(self, request):
        algorithms, datasets = SharesClient().user_shares_grouped(str(request.user))
        return Response({"own_algorithms": algorithms, "own_datasets": datasets})

    @action(
        detail=True,
        methods=["DELETE"],
        name="remove",
        permission_classes=[IsAuthenticated],
    )
    def remove(self, request, pk=None):
        # TODO: this doesn't work anymore :-(
        # needs to fix it and run through SharesClient.
        # In the meantime: always 404
        return Response(status=status.HTTP_404_NOT_FOUND)
