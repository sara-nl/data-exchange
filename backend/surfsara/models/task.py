from django.contrib.postgres.fields import JSONField
from django.db import models
from surfsara.models.permission import Permission


class Task(models.Model):
    REGISTERED = "registered"
    DATA_REQUESTED = "data_requested"
    ANALYZING = "analyzing_algorithm"
    RUNNING = "running"
    SUCCESS = "success"
    ERROR = "error"
    ALGORITHM_CHANGED = "algorithm_changed"
    OUTPUT_RELEASED = "output_released"
    REQUEST_REJECTED = "request_rejected"
    RELEASE_REJECTED = "release_rejected"

    id = models.AutoField(primary_key=True)
    state = models.CharField(
        max_length=255,
        choices=[
            (REGISTERED, "Registered"),
            (DATA_REQUESTED, "Data Requested"),
            (RUNNING, "Running"),
            (ANALYZING, "Analyzing_algorithm"),
            (SUCCESS, "Success"),
            (ERROR, "Error"),
            (ALGORITHM_CHANGED, "Algorithm Changed"),
            (OUTPUT_RELEASED, "Output Released"),
            (REQUEST_REJECTED, "Request Rejected"),
            (RELEASE_REJECTED, "Release Rejected"),
        ],
    )
    author_email = models.EmailField()
    approver_email = models.EmailField()
    algorithm = models.TextField()
    algorithm_etag = models.CharField(max_length=32, default="")
    algorithm_content = JSONField(default=dict)
    algorithm_info = JSONField(default=dict)
    dataset = models.TextField()
    dataset_desc = models.TextField()
    output = models.TextField(null=True)
    review_output = models.BooleanField(default=True)
    permission = models.ForeignKey(Permission, null=True, on_delete=models.SET_NULL)
    registered_on = models.DateTimeField(auto_now_add=True)
