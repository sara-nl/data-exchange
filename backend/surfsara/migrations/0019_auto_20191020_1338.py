# Generated by Django 2.2.6 on 2019-10-20 13:38

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0018_merge_20191020_1240'),
    ]

    operations = [
        migrations.AddField(
            model_name='permission',
            name='review_output',
            field=models.BooleanField(default=True),
        ),
        migrations.AddField(
            model_name='task',
            name='review_output',
            field=models.BooleanField(default=True),
        ),
        migrations.AlterField(
            model_name='task',
            name='state',
            field=models.CharField(choices=[('registered', 'Registered'), ('data_requested', 'Data Requested'), ('running', 'Running'), ('analyzing_algorithm', 'Analyzing_algorithm'), ('success', 'Success'), ('error', 'Error'), ('output_released', 'Output Released'), ('request_rejected', 'Request Rejected'), ('release_rejected', 'Release Rejected')], max_length=255),
        ),
    ]