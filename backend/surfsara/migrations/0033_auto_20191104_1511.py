# Generated by Django 2.2.6 on 2019-11-04 15:11

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0032_merge_20191103_1022")]

    operations = [
        migrations.AlterField(
            model_name="permission",
            name="state",
            field=models.CharField(
                choices=[
                    ("active", "active"),
                    ("rejected", "rejected"),
                    ("aborted", "aborted"),
                ],
                default="active",
                max_length=255,
            ),
        )
    ]
