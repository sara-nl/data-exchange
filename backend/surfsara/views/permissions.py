import datetime
from collections import defaultdict

from django.http import JsonResponse
from django.shortcuts import get_object_or_404
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.parsers import JSONParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from backend.scripts.SharesClient import SharesClient
from surfsara import logger
from surfsara.models import Permission, Task
from surfsara.services import mail_service
from surfsara.services import task_service


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

    shares_client = SharesClient()

    def create(self, request):
        """
        Request a new permission
        """
        data = JSONParser().parse(request)
        data["algorithm_provider"] = request.user.email

        shared_algorithms = self.shares_client.algorithms_shared_by_user(
            request.user.email
        )
        if not f"/{data['algorithm']}" in shared_algorithms:
            return Response("The algorithm is not shared by the user", status=403)

        serializer = PermissionSerializer(data=data)
        if not serializer.is_valid():
            return JsonResponse(serializer.errors, status=400)

        model = serializer.save()
        model.review_output = model.permission_type == Permission.ONE_TIME_PERMISSION
        model.save()

        logger.debug(f"Sending permission {model.id} to analysis")
        task_service.analyze(str(model.id))
        return JsonResponse(PermissionSerializer(model).data)

    def retrieve(self, request, pk=None):
        permission = Permission.objects.get(pk=pk)
        is_approver = permission.dataset_provider == request.user.email
        is_requester = permission.algorithm_provider == request.user.email
        if is_approver or is_requester:
            return Response(PermissionSerializer(permission).data)
        else:
            return Response(status=404)

    @action(
        detail=True,
        methods=["PUT"],
        name="reject",
        permission_classes=[IsAuthenticated],
    )
    def reject(self, request, pk=None):
        logger.debug(f"Rejecting permission {pk}")
        permission = Permission.objects.get(pk=pk)
        if permission.dataset_provider != request.user.email:
            return Response(status=403)
        permission.state = Permission.REJECTED
        permission.status_description = f"Rejected by ${request.user.email}"
        permission.save()

        mail_service.send_mail(
            mail_files="request_reviewed",
            receiver=permission.algorithm_provider,
            subject=f"Your data request was rejected",
            url=f"http://{request.get_host()}/overview",
            reviewable="data request",
            result="rejected",
        )

        return JsonResponse(PermissionSerializer(permission).data)

    @action(
        detail=True,
        methods=["PUT"],
        name="approve",
        permission_classes=[IsAuthenticated],
    )
    def approve(self, request, pk=None):
        logger.debug(f"Accepting permission {pk}")
        permission = Permission.objects.get(pk=pk)

        if permission.dataset_provider != request.user.email:
            return Response(status=403)

        permission.dataset = request.data["dataset"]
        permission.state = Permission.ACTIVE
        permission.save()

        if (
            permission.permission_type == Permission.USER_PERMISSION
            or permission.permission_type == Permission.STREAM_PERMISSION
        ):
            mail_service.send_mail(
                mail_files="permission_granted_do",
                receiver=permission.dataset_provider,
                subject="You granted someone continuous access to your dataset",
                url=f"http://{request.get_host()}/permissions",
                **PermissionSerializer(permission).data,
            )

            mail_service.send_mail(
                mail_files="permission_granted_ao",
                receiver=permission.algorithm_provider,
                subject="You were granted continuous access to a dataset",
                url=f"http://{request.get_host()}/permissions",
                **PermissionSerializer(permission).data,
            )
        else:
            mail_service.send_mail(
                mail_files="request_reviewed",
                receiver=permission.algorithm_provider,
                subject=f"Your data request was approved",
                url=f"http://{request.get_host()}/overview",
                reviewable="data request",
                result="approved",
            )

        return JsonResponse(PermissionSerializer(permission).data)

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
        detail=True,
        methods=["GET"],
        name="head_task_id",
        permission_classes=[IsAuthenticated],
    )
    def head_task_id(self, request, pk=None):
        permission = Permission.objects.get(pk=pk)
        is_approver = permission.dataset_provider == request.user.email
        is_requester = permission.algorithm_provider == request.user.email

        if not (is_approver or is_requester):
            return Response(status=403)

        tasks = Task.objects.filter(permission_id=permission.id).order_by("-id")

        if len(tasks) > 0:
            return Response({"id": tasks[0].id})
        else:
            return Response(None)

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

        alg_shares, _ = SharesClient().user_shares_grouped(str(request.user))
        data = defaultdict(lambda: {"permissions": [], "tasks": []})

        permissions = PermissionSerializer(
            Permission.objects.filter(
                algorithm_provider=request.user.email, state=Permission.ACTIVE
            ),
            many=True,
        ).data

        for permission in permissions:
            if permission["permission_type"] == Permission.USER_PERMISSION:
                for algorithm in alg_shares or []:
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
