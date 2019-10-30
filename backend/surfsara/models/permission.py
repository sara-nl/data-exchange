from django.db import models


class Permission(models.Model):
    USER_PERMISSION = "user permission"
    STREAM_PERMISSION = "stream permission"
    NO_PERMISSION = "no permission"
    ONE_OFF_PERMISSION = "one off permission"

    algorithm = models.TextField(null=True)
    algorithm_provider = models.EmailField()
    dataset = models.TextField()
    dataset_provider = models.EmailField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)
    etag = models.CharField(max_length=255)

    permission_type = models.CharField(
        max_length=255,
        choices=[
            (ONE_OFF_PERMISSION, "one off permisson"),
            (USER_PERMISSION, "user permission"),
            (STREAM_PERMISSION, "stream permission"),
            (NO_PERMISSION, "no permission"),
        ],
        default=NO_PERMISSION,
    )
