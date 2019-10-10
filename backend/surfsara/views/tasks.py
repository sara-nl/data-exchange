from django.db import transaction
from django.db.models import Q
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
import os
import threading

from surfsara.models import User, Task
from surfsara.services import task_service, mail_service
from backend.scripts.run_container import RunContainer

class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Task
        fields = "__all__"


class Tasks(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    # This process has to move to the taskmanager, so it doesn't slow down the site!
    @staticmethod
    def process_algorithm(task, algorithm):
        download_container = RunContainer(algorithm, "", download_dir=os.getcwd())
        download_container.create_files()
        download_container.download_from_rd(data=False)
        algorithm_content = None

        if download_container.temp_algorithm_file:
            with open(download_container.temp_algorithm_file, "r") as algorithm_file:
                algorithm_content = algorithm_file.read()

        download_container.remove_files()
        task.algorithm_content = algorithm_content
        task.save()
    # This process has to move to the taskmanager, so it doesn't slow down the site!

    def create(self, request):
        data_owner_email = request.data["data_owner"]
        if not User.objects.filter(email=data_owner_email):
            return Response({"error": "unknown email"}, status=400)

        task = Task(
            state=Task.DATA_REQUESTED,
            author_email=request.user.email,
            approver_email=data_owner_email,
            algorithm=request.data["algorithm"],
            dataset_desc=request.data["dataset_desc"],
        )
        task.save()

        self.process_algorithm(task, request.data['algorithm'])

        mail_service.send_mail(
            mail_files="data_request",
            receiver=data_owner_email,
            subject="You got a data request",
            url=f"http://{request.get_host()}/review",
        )

        return Response({"owner": True})

    def list(self, request):
        to_approve_requests = Task.objects.filter(
            Q(approver_email=request.user.email),
            Q(state=Task.DATA_REQUESTED) | Q(state=Task.SUCCESS) | Q(state=Task.ERROR),
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

    def retrieve(self, request, pk=None):
        task = Task.objects.get(pk=pk)
        is_owner = task.approver_email == request.user.email
        if task.state != Task.OUTPUT_RELEASED and not is_owner:
            task.output = None

        # download_container = RunContainer(task.algorithm, None)
        # download_container.create_files()
        # download_container.download_from_rd(data=False)
        # algorithm_content = None
        #
        # if download_container.temp_algorithm_file:
        #     with open(download_container.temp_algorithm_file, "r") as algorithm_file:
        #         algorithm_content = algorithm_file.read()

        return Response({"is_owner": is_owner, "algorithm_content": "tetst", **TaskSerializer(task).data})

    @action(detail=True, methods=["POST"], name="review", permission_classes=[AllowAny])
    def review(self, request, pk=None):
        output = ""
        task = Task.objects.get(pk=pk)

        print(task.approver_email)

        if task.approver_email != request.user.email:
            return Response({"output": "Not your file"})

        if request.data["approved"]:
            task.state = Task.RUNNING
            task.dataset = request.data["updated_request"]["dataset"]
            task.save()

            task_service.start(task)
        else:
            task.state = Task.REQUEST_REJECTED

        task.save()

        url = f"http://{request.get_host()}/overview"
        subject = "Your data request has been reviewed"
        mail_service.send_mail("request_reviewed", request.user.email, subject, url=url)

        return Response({"state": task.state, "output": output})

    @action(
        detail=True, methods=["POST"], name="release", permission_classes=[AllowAny]
    )
    def release(self, request, pk=None):
        if not Task.objects.filter(pk=pk, approver_email=request.user.email):
            return Response({"output": "Not your file"})

        task = Task.objects.get(pk=pk)
        if request.data["released"]:
            task.state = Task.OUTPUT_RELEASED
        else:
            task.state = Task.RELEASE_REJECTED
        task.save()

        return Response({"state": task.state})
