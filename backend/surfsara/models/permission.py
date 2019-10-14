from django.db import models


class Permission(models.Model):
    algorithm = models.TextField()
    algorithm_provider = models.TextField()
    dataset = models.TextField()
    dataset_provider = models.TextField()
