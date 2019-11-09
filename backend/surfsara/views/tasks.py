from collections import defaultdict
from django.db.models import Q
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response

from surfsara.models import User, Task, Permission
from surfsara.services import task_service, mail_service
from surfsara.views import permissions


class TaskSerializer(serializers.ModelSerializer):
    permission = permissions.PermissionSerializer(many=False, read_only=True)

    class Meta:
        model = Task
        fields = "__all__"


class Tasks(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def create(self, request):
        data_owner_email = request.data["data_owner"]
        if "@" not in data_owner_email:
            return Response(
                {"error": f"Invalid email address '{data_owner_email}'"}, status=400
            )
        elif not User.objects.filter(email=data_owner_email):
            return Response(
                {"error": f"Unknown email address '{data_owner_email}'"}, status=400
            )

        task = Task(
            state=Task.ANALYZING,
            author_email=request.user.email,
            approver_email=data_owner_email,
            algorithm=request.data["algorithm"],
            dataset_desc=request.data["dataset_desc"],
        )
        task.save()
        task_service.analyze(task)

        mail_service.send_mail(
            mail_files="data_request",
            receiver=data_owner_email,
            subject="You got a data request",
            url=f"http://{request.get_host()}/tasks/{task.pk}",
            author=task.author_email,
            dataset=task.dataset,
        )

        return Response({"owner": True})

    def list(self, request):
        """
        Gets all requests you made and requests on your data that
        are in a state to approve something/
        """

        to_approve_requests = Task.objects.filter(
            Q(approver_email=request.user.email),
            Q(state=Task.DATA_REQUESTED)
            | Q(state=Task.ANALYZING)
            | Q(state=Task.SUCCESS, review_output=True)
            | Q(state=Task.ERROR, review_output=True),
        ).order_by("-registered_on")

        own_requests = Task.objects.filter(author_email=request.user.email).order_by(
            "-registered_on"
        )
        for request in own_requests:
            if request.state != Task.OUTPUT_RELEASED:
                request.output = None

        return Response(
            {
                "to_approve_requests": TaskSerializer(
                    to_approve_requests, many=True
                ).data,
                "own_requests": TaskSerializer(own_requests, many=True).data,
            }
        )

    @action(
        detail=False,
        methods=["GET"],
        name="get_data_requests",
        permission_classes=[IsAuthenticated],
    )
    def get_data_requests(self, request):
        """
        Gets all requests you made and requests on your data that
        are in a state to approve something/
        """

        not_reviewed_yet = Task.objects.filter(
            Q(approver_email=request.user.email),
            Q(state=Task.DATA_REQUESTED)
            | Q(state=Task.SUCCESS, review_output=True)
            | Q(state=Task.ERROR, review_output=True),
        ).order_by("-registered_on")

        reviewed = Task.objects.filter(
            Q(author_email=request.user.email),
            Q(state=Task.RUNNING)
            | Q(state=Task.OUTPUT_RELEASED)
            | Q(state=Task.RELEASE_REJECTED)
            | Q(state=Task.REQUEST_REJECTED)
            | Q(state=Task.SUCCESS, review_output=False)
            | Q(state=Task.ERROR, review_output=False),
        ).order_by("-registered_on")

        # print(TaskSerializer(reviewed, many=True).data)
        # for request in not_reviewed_yet:
        #     if request.state != Task.OUTPUT_RELEASED:
        #         request.output = None

        return Response(
            {
                "not_reviewed_yet": TaskSerializer(not_reviewed_yet, many=True).data,
                "reviewed": TaskSerializer(reviewed, many=True).data,
            }
        )

    @action(
        detail=False, methods=["GET"], name="list_logs", permission_classes=[AllowAny]
    )
    def list_logs(self, request):
        data_tasks_per_file = defaultdict(list)
        algorithm_tasks_per_file = defaultdict(list)

        algorithm_tasks = Task.objects.filter(author_email=request.user.email).order_by(
            "-registered_on"
        )

        data_tasks = Task.objects.filter(approver_email=request.user.email).order_by(
            "-registered_on"
        )

        for request in algorithm_tasks:
            if request.state != Task.OUTPUT_RELEASED:
                request.output = None

        data_tasks = TaskSerializer(data_tasks, many=True).data
        for perm in data_tasks:
            data_tasks_per_file[perm["dataset"]].append(perm)

        algorithm_tasks = TaskSerializer(algorithm_tasks, many=True).data
        for perm in algorithm_tasks:
            algorithm_tasks_per_file[perm["algorithm"]].append(perm)

        return Response(
            {
                "algorithm_tasks": algorithm_tasks_per_file,
                "data_tasks": data_tasks_per_file,
            }
        )

    def retrieve(self, request, pk=None):
        """
        Gets specific task
        """

        task = Task.objects.get(pk=pk)
        is_owner = task.approver_email == request.user.email
        if (
            task.state != Task.OUTPUT_RELEASED
            and not (task.state == Task.ERROR and task.review_output is False)
            and not is_owner
        ):
            task.output = None

        return Response({"is_owner": is_owner, **TaskSerializer(task).data})

    @action(
        detail=True,
        methods=["POST"],
        name="review",
        permission_classes=[IsAuthenticated],
    )
    def review(self, request, pk=None):
        """
        Processes review of request made by algorithm provider and reviewed
        by data provider
        """
        task = Task.objects.get(pk=pk)

        if task.approver_email != request.user.email:
            return Response({"output": "Not your file"})

        update = request.data["updated_request"]
        if request.data["approved"]:
            result = "approved"
            permission_type = Permission.ONE_TIME_PERMISSION
            algorithm_name = task.algorithm

            task.dataset = update["dataset"]
            task.review_output = request.data["review_output"]
            task_service.start(task)
            task.state = Task.RUNNING
            task.save()

            if request.data["approve_user"] or request.data["stream"]:
                mail_service.send_mail(
                    mail_files="permission_granted_do",
                    receiver=task.approver_email,
                    subject="You granted someone continuous access to your dataset",
                    url=f"http://{request.get_host()}/permissions",
                    **update,
                )

                mail_service.send_mail(
                    mail_files="permission_granted_ao",
                    receiver=task.author_email,
                    subject="You were granted continuous access to a dataset",
                    url=f"http://{request.get_host()}/permissions",
                    **update,
                )
                if request.data["approve_user"]:
                    permission_type = Permission.USER_PERMISSION
                    algorithm_name = "Any algorithm"
                elif request.data["stream"]:
                    permission_type = Permission.STREAM_PERMISSION
                else:
                    raise AssertionError("Invalid state - this should never be reached")

            new_perm = Permission(
                algorithm=algorithm_name,
                algorithm_provider=update["author_email"],
                algorithm_etag=task.algorithm_etag,
                dataset=update["dataset"],
                dataset_provider=update["approver_email"],
                review_output=request.data["review_output"],
                permission_type=permission_type,
                state=Permission.ACTIVE,
            )
            new_perm.save()
            task.permission = new_perm
            task.save()
        else:
            result = "rejected"
            task.state = Task.REQUEST_REJECTED
            task.save()

        mail_service.send_mail(
            mail_files="request_reviewed",
            receiver=update["author_email"],
            subject=f"Your data request was {result}",
            url=f"http://{request.get_host()}/overview",
            reviewable="data request",
            result=result,
        )

        return Response({"state": task.state, "output": task.output})

    @action(
        detail=True,
        methods=["POST"],
        name="release",
        permission_classes=[IsAuthenticated],
    )
    def release(self, request, pk=None):
        """
        Processes release or not release of output
        """

        if not Task.objects.filter(pk=pk, approver_email=request.user.email):
            return Response({"output": "Not your file"})

        task: Task = Task.objects.get(pk=pk)
        if request.data["released"]:
            task.state = Task.OUTPUT_RELEASED
            result = "approved"
        else:
            task.state = Task.RELEASE_REJECTED
            result = "rejected"
        task.save()

        mail_service.send_mail(
            mail_files="request_reviewed",
            receiver=task.author_email,
            subject=f"The output of your task was {result}",
            url=f"http://{request.get_host()}/tasks/{task.pk}",
            reviewable="algorithm output",
            result=result,
        )

        return Response({"state": task.state})

    @action(
        detail=True,
        methods=["POST"],
        name="start_with_perm",
        permission_classes=[IsAuthenticated],
    )
    def start_with_perm(self, request, pk=None):
        perm = Permission.objects.get(
            id=pk,
            algorithm_provider=request.user.email,
            dataset_provider=request.data["dataset_provider"],
            dataset=request.data["dataset"],
        )

        if not perm:
            return Response({"output": "You don't have this permission"})

        task = Task(
            state=Task.ANALYZING,
            author_email=perm.algorithm_provider,
            approver_email=perm.dataset_provider,
            algorithm=request.data["algorithm"],
            dataset=perm.dataset,
            review_output=perm.review_output,
            dataset_desc="",
            permission=perm,
        )
        task.save()
        task_service.analyze(task)
        task_service.start(task)
        task.state = Task.RUNNING
        task.save()

        return Response({"id": task.id})
