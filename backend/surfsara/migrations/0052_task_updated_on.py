# Generated by Django 2.2.7 on 2019-12-06 10:29

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("surfsara", "0051_auto_20191205_1227"),
    ]

    operations = [
        migrations.AddField(
            model_name="task",
            name="updated_on",
            field=models.DateTimeField(auto_now=True),
        ),
    ]
