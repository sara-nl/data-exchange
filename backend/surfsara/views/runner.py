import jsonschema
from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from rest_framework.decorators import api_view
from rest_framework.exceptions import ParseError
from rest_framework.metadata import BaseMetadata
from rest_framework.parsers import JSONParser
from rest_framework.permissions import BasePermission, IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework.schemas import ManualSchema

from surfsara.models import User
from backend.scripts.run_container import RunContainer


class StartParser(JSONParser):
    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["algorithm_file", "data_file", "username", "password"],
        "properties": {
            "algorithm_file": {
                "$id": "#/properties/algorithm_file",
                "type": "string",
                "minLength": 1,
            },
            "data_file": {
                "$id": "#/properties/data_file",
                "type": "string",
                "minLength": 1,
            },
            "username": {
                "$id": "#/properties/username",
                "type": "string",
                "minLength": 1,
            },
            "password": {
                "$id": "#/properties/password",
                "type": "string",
                "minLength": 1,
            },
        },
    }

    def parse(self, stream, media_type=None, parser_context=None):
        data = super(StartParser, self).parse(stream, media_type, parser_context)
        try:
            jsonschema.validate(data, self.schema)
        except ValueError as error:
            raise ParseError(detail=error.message)
        return data


class StartViewSet(viewsets.ViewSet):
    permission_classes = (AllowAny,)
    parser_classes = (StartParser,)

    def create(self, request):
        runner = RunContainer(
            algorithm_file_name=request.data["algorithm_file"],
            data_file_name=request.data["data_file"],
            download_dir="/tmp/surfsara",
        )
        runner.download_files(request.data["username"], request.data["password"])
        file = runner.run_algorithm()

        with open(file, "r") as f:
            output = f.read()

        return Response({"output": output})
