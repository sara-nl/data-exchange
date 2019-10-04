from django.db import models


class Task(models.Model):
    id = models.AutoField(primary_key=True)
    state = models.CharField(max_length=255)
    author_email = models.TextField()
    approver_email = models.TextField()
    algorithm = models.TextField()
    dataset = models.TextField()
    dataset_desc = models.TextField()
    output = models.TextField()
    registered_on = models.DateTimeField(auto_now_add=True)
