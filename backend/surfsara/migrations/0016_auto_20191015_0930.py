# Generated by Django 2.2.6 on 2019-10-15 09:30

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0015_auto_20191011_2328")]

    operations = [
        migrations.RemoveField(model_name="task", name="review_output"),
        migrations.AddField(
            model_name="task",
            name="algorithm_content",
            field=models.TextField(default="No algorithm"),
        ),
        migrations.AddField(
            model_name="task", name="algorithm_info", field=models.TextField(default="")
        ),
    ]
