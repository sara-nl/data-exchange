# Generated by Django 2.2.6 on 2019-10-26 11:37

import django.contrib.postgres.fields.jsonb
from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0020_auto_20191026_1129'),
    ]

    operations = [
        migrations.AlterField(
            model_name='task',
            name='algorithm_content',
            field=django.contrib.postgres.fields.jsonb.JSONField(default=dict),
        ),
    ]
