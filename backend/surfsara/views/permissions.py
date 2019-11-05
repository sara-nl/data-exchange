from collections import defaultdict

from django.shortcuts import get_object_or_404
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response

from surfsara.models import Permission, User, Task
from surfsara.services import mail_service
from surfsara.services.files_service import OwnShares

import datetime


class PermissionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permission
        fields = "__all__"


class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Task
        fields = "__all__"


class Permissions(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def list(self, request):
        """
        Gives list of obtained and given permissions
        """

        obtained_permissions = Permission.objects.filter(
            algorithm_provider=request.user.email
        )
        given_permissions = Permission.objects.filter(
            dataset_provider=request.user.email
        )

        obtained_permissions = PermissionSerializer(
            obtained_permissions, many=True
        ).data
        given_permissions = PermissionSerializer(given_permissions, many=True).data

        return Response(
            {
                "obtained_permissions": obtained_permissions,
                "given_permissions": given_permissions,
            }
        )

    @action(
        detail=False,
        methods=["GET"],
        name="obtained_per_file",
        permission_classes=[IsAuthenticated],
    )
    def obtained_per_file(self, request):
        """
        Returns list permissions per file in dict
        """

        alg_shares, _ = OwnShares(str(request.user)).return_own_shares()
        data = defaultdict(lambda: {"permissions": [], "tasks": []})

        permissions = PermissionSerializer(
            Permission.objects.filter(
                algorithm_provider=request.user.email, state=Permission.ACTIVE
            ),
            many=True,
        ).data

        for permission in permissions:
            if permission["permission_type"] == Permission.USER_PERMISSION:
                for algorithm in alg_shares:
                    file_ = algorithm["file_target"].strip("/")
                    data[file_]["permissions"].append(permission)
            else:
                file_ = permission["algorithm"]
                data[file_]["permissions"].append(permission)

        # Append the log of tasks
        tasks = TaskSerializer(
            Task.objects.filter(author_email=request.user.email).order_by(
                "-registered_on"
            ),
            many=True,
        ).data

        for task in tasks:
            data[task["algorithm"]]["tasks"].append(task)

        return Response(data)

    @action(
        detail=False,
        methods=["GET"],
        name="given_per_file",
        permission_classes=[IsAuthenticated],
    )
    def given_per_file(self, request):
        """
        Returns list permissions per file in dict
        """

        given_per_file = defaultdict(list)

        given_permissions = Permission.objects.filter(
            dataset_provider=request.user.email, state=Permission.ACTIVE
        )

        given_permissions = PermissionSerializer(given_permissions, many=True).data

        for perm in given_permissions:
            given_per_file[perm["dataset"]].append(perm)

        return Response({"given_permissions": given_per_file})

    @action(
        detail=False,
        methods=["GET"],
        name="list_permissions",
        permission_classes=[IsAuthenticated],
    )
    def list_permissions(self, request):
        """
        Returns all unique permissions
        """
        permissions = [permission[1] for permission in
                       Permission.PERMISSIONS if permission[0] != Permission.NO_PERMISSION]
        return Response({"list_permissions": permissions})

    @action(
        detail=True,
        methods=["POST"],
        name="remove",
        permission_classes=[IsAuthenticated],
    )
    def remove(self, request, pk=None):
        """
        Removes permission from database
        """

        permission: Permission = get_object_or_404(
            Permission, pk=pk, dataset_provider=request.user.email
        )
        permission.state = Permission.REJECTED
        permission.status_description = datetime.datetime.now()

        permission.save()

        mail_service.send_mail(
            "permission_revoked",
            permission.algorithm_provider,
            "Permission revoked for dataset",
            dataset=permission.dataset,
            url=f"http://{request.get_host()}/permissions",
        )

        return self.list(request)
