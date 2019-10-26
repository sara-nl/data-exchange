from django.shortcuts import get_object_or_404
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response

from surfsara.models import Permission, User
from surfsara.services import mail_service


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
        obtained_permissions = Permission.objects.filter(
            algorithm_provider=request.user.email
        )
        given_permissions = Permission.objects.filter(
            dataset_provider=request.user.email
        )

        obtained_permissions = PermissionSerializer(
            obtained_permissions, many=True
        ).data
        obtained_per_file = {}
        for perm in obtained_permissions:
            if perm["algorithm"] in obtained_per_file:
                obtained_per_file[perm["algorithm"]].append(perm)
            else:
                obtained_per_file[perm["algorithm"]] = [perm]

        given_permissions = PermissionSerializer(given_permissions, many=True).data
        given_per_file = {}
        for perm in given_permissions:
            if perm["algorithm"] in given_per_file:
                given_per_file[perm["algorithm"]].append(perm)
            else:
                given_per_file[perm["algorithm"]] = [perm]

        return Response(
            {
                "obtained_permissions": obtained_per_file,
                "given_permissions": given_per_file,
            }
        )

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
        permission.delete()

        mail_service.send_mail(
            "permission_revoked",
            permission.algorithm_provider,
            "Permission revoked for dataset",
            dataset=permission.dataset,
            url=f"http://{request.get_host()}/permissions",
        )

        return self.list(request)
