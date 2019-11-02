from django.shortcuts import get_object_or_404
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response

from surfsara.models import Permission, User
from surfsara.services import mail_service
from surfsara.services.files_service import OwnShares

import datetime


class PermissionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permission
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
        name="per_file",
        permission_classes=[IsAuthenticated],
    )
    def per_file(self, request):
        """
        Returns list permissions per file in dict
        """

        alg_shares, _ = OwnShares(str(request.user)).return_own_shares()

        obtained_per_file = {}
        given_per_file = {}

        obtained_permissions = Permission.objects.filter(
            algorithm_provider=request.user.email, state=Permission.ACTIVE
        )

        obtained_permissions = PermissionSerializer(
            obtained_permissions, many=True
        ).data
        for perm in obtained_permissions:
            if perm["permission_type"] == Permission.USER_PERMISSION:
                for algorithm in alg_shares:
                    add_per_file(
                        algorithm["file_target"].strip("/"), obtained_per_file, perm
                    )
            else:
                add_per_file(perm["algorithm"], obtained_per_file, perm)

        return Response({"obtained_permissions": obtained_per_file})

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


def add_per_file(item, per_file_dict, perm):
    if item in per_file_dict:
        per_file_dict[item].append(perm)
    else:
        per_file_dict[item] = [perm]
