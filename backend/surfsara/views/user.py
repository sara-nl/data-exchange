from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from rest_framework.decorators import api_view
from surfsara.models import User


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
            fields = ["url", "username", "email", "is_staff", "webdav_username"]

    queryset = User.objects.all()
    serializer_class = Serializer
