import jsonschema
import json

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
from backend.scripts.ResearchdriveClient import ResearchdriveClient


class StartParser(JSONParser):
    """Parser to check if the JSON sent to the endpoint is valid."""

    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["algorithm_file", "data_file"],
        "properties": {
            "algorithm_file": {"type": "string", "minLength": 1},
            "data_file": {"type": "string", "minLength": 1}
        },
    }

    def parse(self, stream, media_type=None, parser_context=None):
        data = super(StartParser, self).parse(
            stream, media_type, parser_context)
        try:
            jsonschema.validate(data, self.schema)
        except ValueError as error:
            raise ParseError(detail=error.message)
        return data


class StartViewSet(viewsets.ViewSet):
    """View for the /runner/start endpoint."""

    permission_classes = [IsAuthenticated]
    parser_classes = [StartParser]

    def create(self, request):
        print(request.data["algorithm_file"])
        print(request.data["data_file"])

        runner = RunContainer(
            remote_algorithm_path=request.data["algorithm_file"],
            remote_data_path=request.data["data_file"],
            download_dir="./files",
        )

        # try:
        runner.download_files()
        file  = runner.run_algorithm()
        with open(file, "r") as f:
            output = f.read()
        # except Exception as error:
        #     output = "Could not locate file.\nPlease refresh for up-to-date files."

        return Response({"output": output})


class ViewShares(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        rd_client = ResearchdriveClient()
        return Response({"output": rd_client.get_shares()})


class ViewSharesPerson(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        algorithms = []
        datasets = []
        errors = []

        print(request.user)

        rd_client = ResearchdriveClient()
        shares = rd_client.get_shares()

        for share in shares:
            filename = share.get("file_target").strip("/")
            if filename[-2:] == "py":
                if share.get("uid_owner") == str(request.user):
                    algorithms.append(filename)
            elif filename[-3:] == "txt":
                datasets.append(filename)

        if not algorithms:
            errors.append("You have no available algorithms\n")

        if not datasets:
            errors.append("You have no available datasets\n")

        print(errors)

        return Response({"output": {
            "algorithms": algorithms,
            "datasets": datasets,
            "errors": errors}
        })
