# Generated by Django 2.2.7 on 2019-12-06 13:22

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("surfsara", "0052_task_updated_on"),
    ]

    operations = [
        migrations.RemoveField(model_name="task", name="algorithm_content",),
        migrations.RemoveField(model_name="task", name="algorithm_etag",),
        migrations.RemoveField(model_name="task", name="algorithm_info",),
        migrations.RemoveField(model_name="task", name="dataset_desc",),
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
                    ("request_rejected", "Request Rejected"),
                    ("release_rejected", "Release Rejected"),
                ],
                max_length=255,
            ),
        ),
    ]
