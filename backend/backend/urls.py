from django.contrib import admin
from django.urls import path, include
from rest_framework import routers, serializers, viewsets
from rest_framework.authtoken.views import obtain_auth_token

from surfsara.views import user, runner

# Routers provide an easy way of automatically determining the URL conf.
router = routers.DefaultRouter()
router.register("users", user.UserViewSet)
router.register("runner/start", runner.StartViewSet, basename="runner")
router.register("runner/shares", runner.ViewShares, basename="shares") 

urlpatterns = [
    path("api/", include(router.urls)),
    path("admin/", admin.site.urls),
    path("api-auth-token/", obtain_auth_token),
    path("api-auth/", include("rest_framework.urls")),
]
