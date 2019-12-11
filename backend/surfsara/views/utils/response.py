from rest_framework.response import Response


def error_400(msg: str):
    return Response({"error": msg}, status=400)
