# Generated by Django 2.2.6 on 2019-10-11 21:28

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0012_permission")]

    operations = [
        migrations.AddField(
            model_name="permission",
            name="algorithm_provider",
            field=models.TextField(default=""),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name="permission",
            name="dataset_provider",
            field=models.TextField(default=""),
            preserve_default=False,
        ),
    ]