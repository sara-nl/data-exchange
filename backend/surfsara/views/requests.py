from collections import defaultdict
from django.db.models import Q
import logging
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.parsers import JSONParser
from django.http import HttpResponse, JsonResponse


from surfsara.models import User, Task, Permission
from surfsara.services import task_service
from surfsara.views import permissions

from surfsara.views.utils.response import error_400


class PermissionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permission
        fields = "__all__"


class Requests(viewsets.ViewSet):

    permission_classes = (IsAuthenticated,)

    @action(
        detail=False,
        methods=["GET"],
        name="submitted",
        permission_classes=[IsAuthenticated],
    )
    def submitted(self, request):
        permissions = Permission.objects.filter(
            Q(dataset_provider=request.user.email)
        ).order_by("-id")

        serializer = PermissionSerializer(permissions, many=True)
        return JsonResponse(serializer.data, safe=False)

    @action(
        detail=False,
        methods=["GET"],
        name="received",
        permission_classes=[IsAuthenticated],
    )
    def received(self, request):
        permissions = Permission.objects.filter(
            Q(dataset_provider=request.user.email)
        ).order_by("-id")

        serializer = PermissionSerializer(permissions, many=True)
        return JsonResponse(serializer.data, safe=False)

    @action(
        detail=False,
        methods=["PUT"],
        name="approve",
        permission_classes=[IsAuthenticated],
    )
    def approve(self, request):

        dataset = request.data["dataset"]
        permission_id = request.data["permission_id"]

        logging.debug(
            f"Approving access request {permission_id} for use of dataset {dataset}"
        )

        user_requests = Permission.objects.filter(
            Q(dataset_provider=request.user.email)
        ).order_by("-id")

        serializer = PermissionSerializer(user_requests, many=True)
        return JsonResponse(serializer.data, safe=False)
