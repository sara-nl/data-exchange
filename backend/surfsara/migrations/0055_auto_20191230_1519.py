# Generated by Django 2.2.8 on 2019-12-30 15:19

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("surfsara", "0054_task_progress_state"),
    ]

    operations = [
        migrations.AlterField(
            model_name="task",
            name="state",
            field=models.CharField(
                choices=[
                    ("stream_permission_request", "stream_permission_request"),
                    ("data_requested", "Data Requested"),
                    ("running", "Running"),
                    ("success", "Success"),
                    ("error", "Error"),
                    ("output_released", "Output Released"),
                    ("release_rejected", "Release Rejected"),
                ],
                max_length=255,
            ),
        ),
    ]
