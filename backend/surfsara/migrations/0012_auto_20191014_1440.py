# Generated by Django 2.2.6 on 2019-10-14 14:40

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0011_auto_20191007_1341")]

    operations = [
        migrations.AddField(
            model_name="task",
            name="algorithm_content",
            field=models.TextField(default="No algorithm"),
        ),
        migrations.AddField(
            model_name="task", name="algorithm_info", field=models.TextField(default="")
        ),
    ]
