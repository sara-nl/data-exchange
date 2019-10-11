from django.db import models


class Task(models.Model):
    REGISTERED = "registered"
    DATA_REQUESTED = "data_requested"
    RUNNING = "running"
    SUCCESS = "success"
    ERROR = "error"
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
            (SUCCESS, "Success"),
            (ERROR, "Error"),
            (OUTPUT_RELEASED, "Output Released"),
            (REQUEST_REJECTED, "Request Rejected"),
            (RELEASE_REJECTED, "Release Rejected"),
        ],
    )
    author_email = models.TextField()
    approver_email = models.TextField()
    algorithm = models.TextField()
    dataset = models.TextField()
    dataset_desc = models.TextField()
    output = models.TextField(null=True)
    registered_on = models.DateTimeField(auto_now_add=True)
    review_output = models.BooleanField(default=True)
