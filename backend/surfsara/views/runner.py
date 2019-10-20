import jsonschema

from rest_framework import viewsets
from rest_framework.exceptions import ParseError
from rest_framework.parsers import JSONParser
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from backend.scripts.ResearchdriveClient import ResearchdriveClient


class StartParser(JSONParser):
    """Parser to check if the JSON sent to the endpoint is valid."""

    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["algorithm_file", "data_file"],
        "properties": {
            "algorithm_file": {"type": "string", "minLength": 1},
            "data_file": {"type": "string", "minLength": 1},
        },
    }

    def parse(self, stream, media_type=None, parser_context=None):
        data = super(StartParser, self).parse(stream, media_type, parser_context)
        try:
            jsonschema.validate(data, self.schema)
        except ValueError as error:
            raise ParseError(detail=error.message)
        return data


class ViewFileVersions(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def create(self, request):
        rd_client = ResearchdriveClient()
        shares = rd_client.get_shares()

        # Get the first share:
        remote_path = shares[10]["path"]
        file_id = shares[10]["file_source"]

        result = {"file_versions": rd_client.get_file_versions(file_id, remote_path)}
        return Response(result)
