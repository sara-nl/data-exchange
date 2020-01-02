from collections import defaultdict
from django.db.models import Q
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response

from surfsara.models import User, Task, Permission
from surfsara.services import task_service, mail_service
from surfsara.views import permissions
from surfsara import logger


class TaskSerializer(serializers.ModelSerializer):
    permission = permissions.PermissionSerializer(many=False, read_only=True)

    class Meta:
        model = Task
        fields = "__all__"


class Tasks(viewsets.ViewSet):
    permission_classes = (IsAuthenticated,)

    def list(self, request):
        """
        Gets all requests you made and requests on your data that
        are in a state to approve something/
        """

        to_approve_requests = Task.objects.filter(
            Q(approver_email=request.user.email),
            Q(state=Task.DATA_REQUESTED)
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
            | Q(state=Task.SUCCESS, review_output=False)
            | Q(state=Task.ERROR, review_output=False),
        ).order_by("-registered_on")

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
            data_tasks_per_file[perm["dataset"].split("/")[0]].append(perm)

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
        is_author = task.author_email == request.user.email

        if not is_owner and not is_author:
            return Response(status=403)

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
            task.permission.state = Permission.ABORTED
            task.permission.save()
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
            id=pk, dataset_provider=request.user.email, state=Permission.ACTIVE
        )

        if not perm:
            return Response(status=403)

        if perm.permission_type == Permission.STREAM_PERMISSION:
            return Response(
                "Tasks with streaming permissions should not be started from frontend",
                status=406,
            )

        if perm.permission_type == Permission.USER_PERMISSION:
            return Response(
                "This endpoint can be used only for One time permissions", status=406
            )

        task = Task(
            state=Task.RUNNING,
            author_email=perm.algorithm_provider,
            approver_email=perm.dataset_provider,
            algorithm=perm.algorithm,
            dataset=perm.dataset,
            review_output=perm.review_output,
            permission=perm,
        )

        task.save()

        logger.info(f"Created new task {task.id} from one time permission {perm.id}")
        task_service.start(task)

        return Response(TaskSerializer(task).data)

    @action(
        detail=True,
        methods=["POST"],
        name="start_with_user_perm",
        permission_classes=[IsAuthenticated],
    )
    def start_with_user_perm(self, request, pk=None):
        perm = Permission.objects.get(
            id=pk, algorithm_provider=request.user.email, state=Permission.ACTIVE
        )

        if not perm:
            return Response(status=403)

        if perm.permission_type != Permission.USER_PERMISSION:
            return Response(
                "This endpoint can be used only for User permissions", status=406
            )

        task = Task(
            state=Task.RUNNING,
            author_email=perm.algorithm_provider,
            approver_email=perm.dataset_provider,
            algorithm=request.data["algorithm"],
            dataset=perm.dataset,
            review_output=False,  # Because it's User Permission
            permission=perm,
        )

        task.save()

        logger.info(f"Created new task {task.id} from user permission {perm.id}")
        task_service.start(task)

        return Response(TaskSerializer(task).data)
