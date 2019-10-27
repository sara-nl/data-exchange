from django.db import models


class Permission(models.Model):
    algorithm = models.TextField()
    algorithm_provider = models.EmailField()
    dataset = models.TextField()
    dataset_provider = models.EmailField()
    review_output = models.BooleanField(default=True)
    registered_on = models.DateTimeField(auto_now_add=True)
