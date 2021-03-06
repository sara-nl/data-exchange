# fmt: off
# Generated by Django 2.2.5 on 2019-10-04 16:32

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0008_task_dataset_desc")]

    operations = [
        migrations.AlterField(
            model_name="task",
            name="state",
            field=models.CharField(
                choices=[
                    ("registered", "Registered"),
                    ("data_requested", "Data Requested"),
                    ("running", "Running"),
                    ("success", "Success"),
                    ("output_released", "Output Released"),
                    ("request_rejected", "Request Rejected"),
                    ("release_rejected", "Release Rejected"),
                ],
                max_length=255,
            ),
        )
    ]
