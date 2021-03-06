# Generated by Django 2.2.6 on 2019-11-07 10:52

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0035_merge_20191104_1512")]

    operations = [
        migrations.AlterField(
            model_name="task",
            name="state",
            field=models.CharField(
                choices=[
                    ("registered", "Registered"),
                    ("data_requested", "Data Requested"),
                    ("running", "Running"),
                    ("analyzing_algorithm", "Analyzing_algorithm"),
                    ("success", "Success"),
                    ("error", "Error"),
                    ("algorithm_changed", "Algorithm Changed"),
                    ("output_released", "Output Released"),
                    ("request_rejected", "Request Rejected"),
                    ("release_rejected", "Release Rejected"),
                ],
                max_length=255,
            ),
        )
    ]
