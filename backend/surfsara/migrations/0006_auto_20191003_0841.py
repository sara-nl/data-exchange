# Generated by Django 2.2.6 on 2019-10-03 08:41

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0005_task'),
    ]

    operations = [
        migrations.AddField(
            model_name='task',
            name='algorithm',
            field=models.TextField(null=True),
        ),
        migrations.AddField(
            model_name='task',
            name='dataset',
            field=models.TextField(null=True),
        ),
    ]
