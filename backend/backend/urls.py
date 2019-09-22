from django.contrib import admin
from django.urls import path, include
from rest_framework import routers, serializers, viewsets

from surfsara.views import UserViewSet

# Routers provide an easy way of automatically determining the URL conf.
router = routers.DefaultRouter()
router.register("users", UserViewSet)


urlpatterns = [
    path("api/", include(router.urls)),
    path("admin/", admin.site.urls),
    path("api-auth/", include("rest_framework.urls")),
]
