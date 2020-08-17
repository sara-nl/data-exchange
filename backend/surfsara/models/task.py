from django.contrib.postgres.fields import JSONField
from django.db import models
from surfsara.models.permission import Permission


class Task(models.Model):
    STREAM_PERMISSION_REQUEST = "stream_permission_request"
    DATA_REQUESTED = "data_requested"
    RUNNING = "running"
    SUCCESS = "success"
    ERROR = "error"
    OUTPUT_RELEASED = "output_released"
    RELEASE_REJECTED = "release_rejected"

    TASK_STATES = (
        (STREAM_PERMISSION_REQUEST, "stream_permission_request"),
        (DATA_REQUESTED, "Data Requested"),
        (RUNNING, "Running"),
        (SUCCESS, "Success"),
        (ERROR, "Error"),
        (OUTPUT_RELEASED, "Output Released"),
        (RELEASE_REJECTED, "Release Rejected"),
    )

    id = models.AutoField(primary_key=True)
    state = models.CharField(max_length=255, choices=TASK_STATES)
    progress_state = JSONField(null=True)
    author_email = models.EmailField()
    approver_email = models.EmailField()
    algorithm = models.TextField()
    algorithm_storage = models.TextField()
    dataset = models.TextField()
    dataset_storage = models.TextField()
    output = models.TextField(null=True)
    review_output = models.BooleanField(default=True)
    permission = models.ForeignKey(Permission, null=True, on_delete=models.SET_NULL)
    registered_on = models.DateTimeField(auto_now_add=True)
    updated_on = models.DateTimeField(auto_now=True)
