from rest_framework import viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from django.core.mail import EmailMultiAlternatives
from django.db import transaction
from django.template.loader import render_to_string
from django.core import serializers

from rest_framework.decorators import action

from surfsara.models import User, Task
from surfsara.services.start_runner import start_runner
from surfsara.services.mail_services import send_mail


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

        return Response({})

    def list(self, request):
        user = str(request.user)

        to_approve_requests = Task.objects.filter(approver_email=user, state="data_requested")
        own_requests = Task.objects.filter(author_email=user).values()

        for request in own_requests:
            if not request["state"] == "approved":
                request["output"] = ""

        print(own_requests)

        return Response({"to_approve_requests": to_approve_requests.values(),
                         "own_requests": own_requests})

    @action(
        detail=True,
        methods=["POST"],
        name="review",
        permission_classes=[AllowAny],
    )
    def review(self, request, pk=None):
        user = str(request.user)
        updated_request = request.data["updated_request"]

        task_id = updated_request["id"]
        dataset = updated_request["dataset"]

        if request.data["approved"]:
            result = "approved"

            with transaction.atomic():
                task = Task.objects.get(id=task_id)
                task.state = "running"
                task.dataset = dataset

                task.save()
        else:
            result = "rejected"

            with transaction.atomic():
                task = Task.objects.get(id=task_id)
                task.state = "request_rejected"

                task.save()

        domain = request.get_host()
        url = f"http://{domain}/overview"
        options = {"url": url, "result": result}

        subject = "Your data request has been reviewed"

        send_mail("request_reviewed", user, subject, options)

        return Response({"output":result})
