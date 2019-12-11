from django.contrib.postgres.fields import JSONField
from django.db import models
from surfsara.models.permission import Permission


class Task(models.Model):
    STREAM_PERMISSION_REQUEST = "stream_permission_request"
    DATA_REQUESTED = "data_requested"
    RUNNING = "running"
    SUCCESS = "success"
    ERROR = "error"
    ALGORITHM_CHANGED = "algorithm_changed"
    OUTPUT_RELEASED = "output_released"
    REQUEST_REJECTED = "request_rejected"
    RELEASE_REJECTED = "release_rejected"

    TASK_STATES = (
        (STREAM_PERMISSION_REQUEST, "stream_permission_request"),
        (DATA_REQUESTED, "Data Requested"),
        (RUNNING, "Running"),
        (SUCCESS, "Success"),
        (ERROR, "Error"),
        (OUTPUT_RELEASED, "Output Released"),
        (REQUEST_REJECTED, "Request Rejected"),
        (RELEASE_REJECTED, "Release Rejected"),
    )

    id = models.AutoField(primary_key=True)
    state = models.CharField(max_length=255, choices=TASK_STATES)
    author_email = models.EmailField()
    approver_email = models.EmailField()
    algorithm = models.TextField()
    dataset = models.TextField()
    output = models.TextField(null=True)
    review_output = models.BooleanField(default=True)
    permission = models.ForeignKey(Permission, null=True, on_delete=models.SET_NULL)
    registered_on = models.DateTimeField(auto_now_add=True)
    updated_on = models.DateTimeField(auto_now=True)
