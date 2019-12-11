from django.db import models
from django.contrib.postgres.fields import JSONField


class Permission(models.Model):

    # Possible types
    # ==============
    USER_PERMISSION = "One specific user permission"
    STREAM_PERMISSION = "stream permission"
    ONE_TIME_PERMISSION = "one time permission"

    PERMISSIONS = (
        (USER_PERMISSION, "Any algorithm on a Dataset"),
        (STREAM_PERMISSION, "Run for a stream of data sets"),
        (ONE_TIME_PERMISSION, "Run once"),
    )

    # Possible states
    # ===============
    #                         --> rejected
    #                        /        ^
    #                       /        /
    # analyzing -> pending ->  active -> aborted
    #           \           \
    #            \           --> cancelled (*not implemented*)
    #             --> cancelled

    ANALYZING = "analyzing"
    PENDING = "pending"
    CANCELLED = "cancelled"
    REJECTED = "rejected"
    ACTIVE = "active"
    ABORTED = "aborted"

    STATES = (
        (ANALYZING, "Artefact is being automatically analysed"),
        (PENDING, "Newly created request"),
        (CANCELLED, "Cancelled by requester"),
        (ACTIVE, "Approved by approver"),
        (REJECTED, "Rejected or revoked by approver"),
        (ABORTED, "Automatically aborted by DataExchange system"),
    )

    id = models.AutoField(primary_key=True)
    algorithm = models.TextField(null=True)
    algorithm_etag = models.CharField(max_length=32, null=True)
    algorithm_report = JSONField(null=True)
    algorithm_provider = models.EmailField()
    dataset = models.TextField(null=True)
    dataset_provider = models.EmailField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)
    updated_on = models.DateTimeField(auto_now=True)
    permission_type = models.CharField(max_length=255, choices=PERMISSIONS, null=False)
    state = models.CharField(
        max_length=255, choices=STATES, default=ANALYZING, null=False
    )
    request_description = models.TextField(default="")
    status_description = models.TextField(null=True)
