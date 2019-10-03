from rest_framework import viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from django.core.mail import EmailMultiAlternatives
from django.db import transaction
from django.template.loader import render_to_string

from rest_framework.decorators import action
from rest_framework import serializers

from surfsara.models import User, Task
from surfsara.services.start_runner import start_runner
from surfsara.services.mail_services import send_mail


class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Task
        fields = "__all__"


class Tasks(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        algorithm = request.data["algorithm"]
        data_owner_email = request.data["data_owner"]
        dataset_desc = request.data["dataset_desc"]

        algorithm_provider_email = str(request.user)

        if User.objects.filter(email=data_owner_email):
            with transaction.atomic():
                task = Task(state="data_requested", author_email=algorithm_provider_email, approver_email=data_owner_email, algorithm=algorithm, dataset_desc=dataset_desc, output="")
                task.save()

                domain = request.get_host()
                url = f"http://{domain}/review"
                options = {"url": url}

                subject = "You got a data request"

                send_mail("data_request", data_owner_email, subject, options)

        return Response({"owner":True})


    def list(self, request):
        user = str(request.user)

        to_approve_requests = Task.objects.filter(approver_email=user, state="data_requested")
        own_requests = Task.objects.filter(author_email=user).values()

        for request in own_requests:
            if not request["state"] == "output_released":
                request["output"] = ""

        return Response({"to_approve_requests": to_approve_requests.values(),
                         "own_requests": own_requests})

    def retrieve(self, request, pk=None):
        user = str(request.user)
        owner = False

        task = Task.objects.get(pk=pk)
        if not task.state == "output_released" or task.approver_email != user:
            task.output = ""

        if task.approver_email == user:
            owner = True

        serializer = TaskSerializer(task)
        return Response({"task":serializer.data,
                         "owner": owner})


    @action(
        detail=True,
        methods=["POST"],
        name="review",
        permission_classes=[AllowAny],
    )
    def review(self, request, pk=None):
        user = str(request.user)
        # updated_request = request.data["updated_request"]

        dataset = request.data["updated_request"]["dataset"]

        if not Task.objects.filter(id=pk, approver_email=user):
            return Response({"output": "Not your file"})

        if request.data["approved"]:
            with transaction.atomic():
                print(dataset)
                task = Task.objects.get(id=pk)
                task.state = "running"
                task.dataset = dataset

                task.output = "super vette output"

                task.save()
        else:
            with transaction.atomic():
                task = Task.objects.get(id=pk)
                task.state = "request_rejected"

                task.save()

        domain = request.get_host()
        url = f"http://{domain}/overview"
        options = {"url": url}

        subject = "Your data request has been reviewed"
        send_mail("request_reviewed", user, subject, options)

        #TODO countainer output

        return Response({"state":task.state})

    @action(
        detail=True,
        methods=["POST"],
        name="release",
        permission_classes=[AllowAny],
    )
    def release(self, request, pk=None):
        user = str(request.user)
        state = ""

        if not Task.objects.filter(id=pk, approver_email=user):
            return Response({"output": "Not your file"})

        if request.data["released"]:
            state = "output_released"

            with transaction.atomic():
                task = Task.objects.get(id=pk)
                task.state = "output_released"
                task.save()

            return Response({"state": state})

        else:
            state = "release_rejected"
            with transaction.atomic():
                task = Task.objects.get(id=pk)
                task.state = state

                task.save()


        return Response({"state": state})

