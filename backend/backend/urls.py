from django.contrib import admin
from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from rest_framework.authtoken.views import obtain_auth_token

from surfsara.views import user, tasks, shares, permissions

# Routers provide an easy way of automatically determining the URL conf.
router = routers.DefaultRouter()
router.register("users", user.UserViewSet, basename="user")
router.register("shares", shares.ViewShares, basename="shares")
router.register("tasks", tasks.Tasks, basename="tasks")
router.register("permissions", permissions.Permissions, basename="permissions")


urlpatterns = [
    path("api/", include(router.urls)),
    path("admin/", admin.site.urls),
    path("api-auth/", include("rest_framework.urls")),
]
