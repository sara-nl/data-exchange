from django.db import models


class Permission(models.Model):
    USER_PERMISSION = "user permission"

    algorithm = models.TextField(null=True)
    algorithm_provider = models.TextField()
    dataset = models.TextField()
    dataset_provider = models.TextField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)
    user_permission = models.BooleanField(default=True)

    permission_type = models.CharField(
        max_length=255,
        choices=[
            (USER_PERMISSION, "user permission"),
        ],
        default=USER_PERMISSION
    )
