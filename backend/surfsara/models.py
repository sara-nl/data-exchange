from django.db import models
from django.contrib import admin
from django.contrib.auth.models import AbstractUser, BaseUserManager


class UserManager(BaseUserManager):
    use_in_migrations = True

    def _create_user(self, email, password, **extra_fields):
        """Create and save a User with the given email and password."""
        if not email:
            raise ValueError("The given email must be set")
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.is_staff = extra_fields.get("is_staff", False)
        user.save(using=self._db)
        return user

    def create_user(self, email, password=None, **extra_fields):
        """Create and save a regular User with the given email and password."""
        extra_fields.setdefault("is_staff", False)
        extra_fields.setdefault("is_superuser", False)
        return self._create_user(email, password, **extra_fields)

    def create_superuser(self, email, password, **extra_fields):
        """Create and save a SuperUser with the given email and password."""
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)

        if extra_fields.get("is_staff") is not True:
            raise ValueError("Superuser must have is_staff=True.")
        if extra_fields.get("is_superuser") is not True:
            raise ValueError("Superuser must have is_superuser=True.")

        return self._create_user(email, password, **extra_fields)


class User(AbstractUser):
    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []
    objects = UserManager()

    username = None
    email = models.EmailField(unique=True)

    webdav_username = models.CharField(max_length=64, blank=True)
    webdav_password = models.CharField(max_length=256, blank=True)

class Task(models.Model):
    id = models.AutoField(primary_key=True)
    state = models.CharField(max_length=255)
    author_email = models.TextField()
    approver_email = models.TextField()
    algorithm = models.TextField()
    dataset = models.TextField()
    output = models.TextField()
    registered_on = models.DateTimeField(auto_now_add=True)

admin.site.register(User)
