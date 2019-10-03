from rest_framework import viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from django.core.mail import EmailMultiAlternatives
from django.db import transaction
from django.template.loader import render_to_string

from surfsara.models import User, Task


class Tasks(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        algorithm = request.data["algorithm"]
        data_owner_email = request.data["data_owner"]
        requested_data = request.data["requested_data"]

        algorithm_provider_email = str(request.user)

        if User.objects.filter(email=data_owner_email):
            with transaction.atomic():
                task = Task(state="data_requested", author_email=algorithm_provider_email, approver_email=data_owner_email, algorithm=algorithm, dataset=requested_data, output="")
                task.save()

                domain = request.get_host()
                url = f"http://{domain}/review"
                options = {"url": url}

                subject = "You got a data request"
                text_body = render_to_string("data_request.txt", options)
                html_body = render_to_string("data_request.html", options)

                # TODO: This is perfect for in Celery (or another task queue)! Sending an
                # email takes quite a while, which does not scale _at all_.
                #
                # Sticking this in a task queue will just make the email sending slower,
                # not the entire server.
                message = EmailMultiAlternatives(subject, text_body, to=[data_owner_email])
                message.attach_alternative(html_body, "text/html")
                message.send()

        return Response({})
