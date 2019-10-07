from django.db import transaction
from django.db.models import Q
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from surfsara.models import User, Task
from surfsara.services import task_service, mail_service


class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Task
        fields = "__all__"


class Tasks(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        data_owner_email = request.data["data_owner"]
        if not User.objects.filter(email=data_owner_email):
            return Response({error: "unknown email"}, status=400)

        task = Task(
            state=Task.DATA_REQUESTED,
            author_email=request.user.email,
            approver_email=data_owner_email,
            algorithm=request.data["algorithm"],
            dataset_desc=request.data["dataset_desc"],
        )
        task.save()

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
            Q(state=Task.DATA_REQUESTED) | Q(state=Task.SUCCESS),
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

        return Response({"is_owner": is_owner, **TaskSerializer(task).data})

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
            ## TODO this temporary to show that you can run the container
            output = task_service.start_container(task.algorithm, task.dataset)

            task.state = Task.SUCCESS
            task.output = output
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
