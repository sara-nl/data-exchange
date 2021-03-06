# Generated by Django 2.2.6 on 2019-10-26 12:13

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0019_auto_20191020_1338")]

    operations = [
        migrations.AlterField(
            model_name="permission",
            name="algorithm_provider",
            field=models.EmailField(max_length=254),
        ),
        migrations.AlterField(
            model_name="permission",
            name="dataset_provider",
            field=models.EmailField(max_length=254),
        ),
        migrations.AlterField(
            model_name="task",
            name="approver_email",
            field=models.EmailField(max_length=254),
        ),
        migrations.AlterField(
            model_name="task",
            name="author_email",
            field=models.EmailField(max_length=254),
        ),
    ]
