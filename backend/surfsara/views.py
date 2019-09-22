from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from surfsara.models import User


class UserViewSet(viewsets.ModelViewSet):
    class Serializer(serializers.HyperlinkedModelSerializer):
        class Meta:
            model = User
            fields = ["url", "username", "email", "is_staff", "webdav_username"]

    queryset = User.objects.all()
    serializer_class = Serializer
