from django.contrib import admin
from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from rest_framework.authtoken.views import obtain_auth_token

from surfsara.views import user, runner, loader

# Routers provide an easy way of automatically determining the URL conf.
router = routers.DefaultRouter()
router.register("users", user.UserViewSet, basename="user")
router.register("runner/start", runner.StartViewSet, basename="runner")
router.register("runner/shares", runner.ViewShares, basename="shares")
router.register("runner/user_files", loader.GetUserFiles, basename="user_files")


urlpatterns = [
    path("api/", include(router.urls)),
    path("admin/", admin.site.urls),
    path("api-auth/", include("rest_framework.urls")),
]
