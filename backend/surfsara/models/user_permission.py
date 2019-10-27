from django.db import models


class UserPermission(models.Model):
    algorithm_provider = models.TextField()
    dataset = models.TextField()
    dataset_provider = models.TextField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)
