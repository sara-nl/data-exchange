from django.db import models


class Permission(models.Model):
    USER_PERMISSION = "user permission"
    STREAM_PERMISSION = "stream permission"
    NO_PERMISSION = "no permission"
    ONE_TIME_PERMISSION = "one time permission"

    ACTIVE = "active"
    REJECTED = "rejected"
    ABORTED = "aborted"

    algorithm = models.TextField(null=True)
    algorithm_etag = models.CharField(max_length=32, default="")
    algorithm_provider = models.EmailField()
    dataset = models.TextField()
    dataset_provider = models.EmailField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)

    permission_type = models.CharField(
        max_length=255,
        choices=[
            (ONE_TIME_PERMISSION, "one time permission"),
            (USER_PERMISSION, "user permission"),
            (STREAM_PERMISSION, "stream permission"),
            (NO_PERMISSION, "no permission"),
        ],
        default=NO_PERMISSION,
    )
    state = models.CharField(
        max_length=255,
        choices=[(ACTIVE, "active"), (REJECTED, "rejected"), (ABORTED, "aborted")],
        default=ACTIVE,
    )

    status_description = models.TextField(null=True)
