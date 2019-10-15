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
            Q(algorithm_provider=request.user.email))
        given_permissions = Permission.objects.filter(
            Q(dataset_provider=request.user.email))

        obtained_permissions = TaskSerializer(obtained_permissions, many=True).data
        # obtained_permissions = {x["algorithm"]: x for x in obtained_permissions}
        print(obtained_permissions)

        new = {}
        for perm in obtained_permissions:
            if perm["algorithm"] in new:
                new[perm["algorithm"]].append(perm)
            else:
                new[perm["algorithm"]] = [perm]


        given_permissions = TaskSerializer(given_permissions, many=True).data
        given_permissions = {x["dataset"]: x for x in given_permissions}

        return Response({"obtained_permissions": new,
                         "given_permissions": given_permissions})

    @action(detail=True, methods=["POST"], name="remove", permission_classes=[AllowAny])
    def remove(self, request, pk=None):
        permission = Permission.objects.get(
            pk=pk, dataset_provider=request.user.email)

        if permission:
            permission.delete()
            obtained_permissions = Permission.objects.filter(
                Q(algorithm_provider=request.user.email))
            given_permissions = Permission.objects.filter(
                Q(dataset_provider=request.user.email))

            return Response({"obtained_permissions": TaskSerializer(obtained_permissions, many=True).data,
                             "given_permissions": TaskSerializer(given_permissions, many=True).data})
        return Response({})
