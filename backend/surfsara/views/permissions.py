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
from surfsara.models import Permission, Task, User
from surfsara.services import mail_service
from surfsara.services import task_service
from functools import reduce


class PermissionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permission
        fields = "__all__"


class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Task
        fields = "__all__"


class TaskWithPermissionSerializer(serializers.ModelSerializer):
    permission_type = serializers.StringRelatedField(
        source="permission.permission_type"
    )

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
        if not data["algorithm"] in shared_algorithms:
            return Response("The algorithm is not shared by the user", status=403)

        serializer = PermissionSerializer(data=data)
        if not serializer.is_valid():
            return JsonResponse(serializer.errors, status=400)

        try:
            User.objects.get(email=serializer.validated_data["dataset_provider"])
        except User.DoesNotExist:
            return Response(
                f"{serializer.validated_data['dataset_provider']} needs to create a Data Exchange account first",
                status=400,
            )

        model = serializer.save()
        model.review_output = model.permission_type == Permission.ONE_TIME_PERMISSION
        model.save()

        logger.debug(f"Sending permission {model.id} to analysis")
        task_service.analyze(str(model.id))

        domain = request.get_host()

        scheme = "http" if domain.startswith("localhost") else "https"

        mail_service.send_mail(
            mail_files="data_request",
            receiver=model.dataset_provider,
            subject=f"New data request",
            url=f"{scheme}://{domain}/requests/{model.pk}",
            author=model.algorithm_provider,
        )

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
            url=f"http://{request.get_host()}/requests/{pk}",
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

        permission.dataset = request.data["path"]
        permission.dataset_storage = request.data["storage"]
        permission.state = Permission.ACTIVE
        permission.save()

        if (
            permission.permission_type == Permission.USER_PERMISSION
            or permission.permission_type == Permission.STREAM_PERMISSION
        ):
            mail_service.send_mail(
                mail_files="permission_granted_do",
                receiver=permission.dataset_provider,
                subject="You granted someone access to your dataset",
                url=f"http://{request.get_host()}/manage_data",
                **PermissionSerializer(permission).data,
            )

            mail_service.send_mail(
                mail_files="permission_granted_ao",
                receiver=permission.algorithm_provider,
                subject="You were granted access to a dataset",
                url=f"http://{request.get_host()}/manage_algorithms",
                **PermissionSerializer(permission).data,
            )
        else:
            mail_service.send_mail(
                mail_files="request_reviewed",
                receiver=permission.algorithm_provider,
                subject=f"Your data request was approved",
                url=f"http://{request.get_host()}/manage_algorithms",
                reviewable="data request",
                result="approved",
            )

        return JsonResponse(PermissionSerializer(permission).data)

    def list(self, request):
        """
        Gives list of inbound (current user is a receiver)
        and outbound (current user the one who decides) permissions.
        """

        inbound = Permission.objects.filter(
            algorithm_provider=request.user.email
        ).order_by("-registered_on")
        outbound = Permission.objects.filter(
            dataset_provider=request.user.email
        ).order_by("-registered_on")
        serialized = lambda pp: PermissionSerializer(pp, many=True).data

        return Response(
            {
                "inbound": serialized(inbound),
                "outbound": serialized(outbound),
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

        all_shares = SharesClient().all()

        email = str(request.user)

        user_algs = filter(
            lambda s: s["ownerEmail"].lower() == email.lower() and s["isAlgorithm"],
            all_shares,
        )

        all_permissions = PermissionSerializer(
            Permission.objects.filter(
                algorithm_provider=email, state=Permission.ACTIVE
            ).exclude(permission_type=Permission.ONE_TIME_PERMISSION),
            many=True,
        ).data

        all_tasks = TaskWithPermissionSerializer(
            Task.objects.filter(author_email=email)
            .order_by("-registered_on")
            .select_related("permission"),
            many=True,
        ).data

        def reducer(acc, next):
            def include_permission(p):
                pt = p["permission_type"]
                return pt == Permission.USER_PERMISSION or (
                    p["algorithm"] == next["path"]
                    and p["algorithm_storage"] == next["storage"]
                )

            def include_task(t):
                return t["algorithm"] == next["path"]

            acc[next["path"]] = {
                "share": next,
                "permissions": filter(include_permission, all_permissions),
                "tasks": filter(include_task, all_tasks),
            }
            return acc

        data = reduce(reducer, user_algs, {})

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
        ).exclude(permission_type=Permission.ONE_TIME_PERMISSION)

        given_permissions = PermissionSerializer(given_permissions, many=True).data

        for perm in given_permissions:
            given_per_file[perm["dataset"]].append(perm)

        return Response(given_per_file)

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
