from django.db import models
from django.contrib.auth.models import AbstractUser


class User(AbstractUser):
    webdav_username = models.CharField(max_length=64, blank=True)
    webdav_token = models.CharField(max_length=256, blank=True)
