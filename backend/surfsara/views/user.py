import jsonschema
from base64 import urlsafe_b64encode

from django.urls import path, include
from django.utils.encoding import force_bytes

from rest_framework import routers, serializers, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.parsers import JSONParser
from rest_framework.response import Response
from django.core.mail import EmailMultiAlternatives
from django.template.loader import render_to_string
from django.urls import reverse
from django.db import IntegrityError, transaction
from surfsara.models import User
from surfsara.tokens import account_activation_token


class RegisterParser(JSONParser):
    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["email", "password"],
        "properties": {
            "email": {"type": "string", "pattern": r".+@.+\..+"},
            "password": {"type": "string", "minLength": 1},
        },
    }

    def parse(self, stream, media_type=None, parser_context=None):
        data = super(RegisterParser, self).parse(stream, media_type, parser_context)
        try:
            jsonschema.validate(data, self.schema)
        except ValueError as error:
            raise ParseError(detail=error.message)
        return data


class UserViewSet(viewsets.ModelViewSet):
    class Serializer(serializers.HyperlinkedModelSerializer):
        class Meta:
            model = User
            fields = ["url", "username", "email", "is_staff", "webdav_username"]

    queryset = User.objects.all()
    serializer_class = Serializer

    @action(
        detail=False,
        methods=["POST"],
        name="Register",
        permission_classes=[AllowAny],
        parser_classes=[RegisterParser],
    )
    def register(self, request):
        with transaction.atomic():
            email = request.data["email"]
            user = User(email=email, is_active=False)
            user.set_password(request.data["password"])
            user.save()

            path = self.reverse_action("activate", args=[user.pk])
            token = account_activation_token.make_token(user)
            options = {
                "user": user,
                "domain": request.get_host(),
                "path": path,
                "token": token,
            }

            subject = "Activate your email address"
            text_body = render_to_string("activation_email.txt", options)
            html_body = render_to_string("activation_email.html", options)

            # TODO: This is perfect for in Celery (or another task queue)! Sending an
            # email takes quite a while, which does not scale _at all_.
            #
            # Sticking this in a task queue will just make the email sending slower,
            # not the entire server.
            message = EmailMultiAlternatives(subject, text_body, to=[email])
            message.attach_alternative(html_body, "text/html")
            message.send()

            return Response({})

    @action(
        detail=True, methods=["GET"], name="Activate", permission_classes=[AllowAny]
    )
    def activate(self, request, pk=None):
        user = User.objects.get(pk=pk)
        if user.is_active:
            return Response({"error": "User has already been activated."})

        token = self.request.query_params.get("token", None)
        valid = account_activation_token.check_token(user, token)

        user.is_active = True
        user.save()

        return Response({"valid": valid})
