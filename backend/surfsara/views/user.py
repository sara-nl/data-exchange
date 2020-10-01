import jsonschema
from base64 import urlsafe_b64encode

from django.urls import path, include
from django.utils.encoding import force_bytes

from rest_framework import routers, serializers, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ParseError
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.parsers import JSONParser
from rest_framework.response import Response
from django.db import IntegrityError, transaction
from surfsara.models import User
from surfsara.tokens import account_activation_token
from surfsara.services import mail_service
from rest_framework.authtoken.views import obtain_auth_token


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


class ActivateParser(JSONParser):
    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["token"],
        "properties": {"token": {"type": "string", "minLength": 1}},
    }

    def parse(self, stream, media_type=None, parser_context=None):
        data = super(ActivateParser, self).parse(stream, media_type, parser_context)
        try:
            jsonschema.validate(data, self.schema)
        except ValueError as error:
            raise ParseError(detail=error.message)
        return data


class UserViewSet(viewsets.ModelViewSet):
    """
    Create/Read/Update/Delete view for the User model.
    This creates the API endpoints for /users/.
    """

    class Serializer(serializers.HyperlinkedModelSerializer):
        class Meta:
            model = User

            # Specify the fields that are sent to the user
            # (to avoid sending e.g. the password hash to a client).
            fields = ["url", "email", "is_staff", "webdav_username"]

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

            token = account_activation_token.make_token(user)
            domain = request.get_host()

            scheme = "http" if domain.startswith("localhost") else "https"
            url = f"{scheme}://{domain}/register/activate?pk={user.pk}&token={token}"

            mail_service.send_mail(
                mail_files="account_activation",
                receiver=email,
                subject="Activate your account",
                **{"url": url},
            )

            return Response({})

    @action(
        detail=True,
        methods=["POST"],
        name="Activate",
        permission_classes=[AllowAny],
        parser_classes=[ActivateParser],
    )
    def activate(self, request, pk=None):
        user = User.objects.get(pk=pk)
        if user.is_active:
            return Response(
                {"valid": False, "error": "User has already been activated."}
            )

        token = self.request.data.get("token")
        valid = account_activation_token.check_token(user, token)

        user.is_active = True
        user.save()

        if not valid:
            return Response({"valid": False, "error": "Incorrect token."})
        else:
            return Response({"valid": True})

    @action(
        detail=False, methods=["POST"], name="Log in", permission_classes=[AllowAny]
    )
    def login(self, request):
        return obtain_auth_token(request._request)
