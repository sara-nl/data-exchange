from django.db.models import Q

from rest_framework import viewsets, serializers
from rest_framework.response import Response
from rest_framework.decorators import action

from rest_framework.permissions import AllowAny


from surfsara.models import Permission


class TaskSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permission
        fields = "__all__"


class Permissions(viewsets.ViewSet):
    permission_classes = (AllowAny,)

    def list(self, request):
        obtained_permissions = Permission.objects.filter(
            Q(algorithm_provider=request.user.email)
        )
        given_permissions = Permission.objects.filter(
            Q(dataset_provider=request.user.email)
        )

        obtained_permissions = TaskSerializer(obtained_permissions, many=True).data
        given_permissions = TaskSerializer(given_permissions, many=True).data

        return Response(
            {
                "obtained_permissions": obtained_permissions,
                "given_permissions": given_permissions,
            }
        )

    @action(
        detail=False, methods=["GET"], name="per_file", permission_classes=[AllowAny]
    )
    def per_file(self, request):
        obtained_permissions = Permission.objects.filter(
            Q(algorithm_provider=request.user.email)
        )
        given_permissions = Permission.objects.filter(
            Q(dataset_provider=request.user.email)
        )

        obtained_permissions = TaskSerializer(obtained_permissions, many=True).data
        obtained_per_file = {}
        for perm in obtained_permissions:
            if perm["algorithm"] in obtained_per_file:
                obtained_per_file[perm["algorithm"]].append(perm)
            else:
                obtained_per_file[perm["algorithm"]] = [perm]

        given_permissions = TaskSerializer(given_permissions, many=True).data
        given_per_file = {}

        for perm in given_permissions:
            if perm["algorithm"] in given_per_file:
                given_per_file[perm["algorithm"]].append(perm)
            else:
                given_per_file[perm["algorithm"]] = [perm]

        return Response(
            {
                "obtained_permissions": obtained_per_file,
                "given_permissions": given_per_file,
            }
        )

    @action(detail=True, methods=["POST"], name="remove", permission_classes=[AllowAny])
    def remove(self, request, pk=None):
        permission = Permission.objects.get(pk=pk, dataset_provider=request.user.email)

        if permission:
            permission.delete()
            obtained_permissions = Permission.objects.filter(
                Q(algorithm_provider=request.user.email)
            )
            given_permissions = Permission.objects.filter(
                Q(dataset_provider=request.user.email)
            )

            return Response(
                {
                    "obtained_permissions": TaskSerializer(
                        obtained_permissions, many=True
                    ).data,
                    "given_permissions": TaskSerializer(
                        given_permissions, many=True
                    ).data,
                }
            )
        return Response({})
