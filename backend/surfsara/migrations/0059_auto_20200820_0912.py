# Generated by Django 2.2.15 on 2020-08-20 09:12

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0058_auto_20200813_0911'),
    ]

    operations = [
        migrations.AlterField(
            model_name='permission',
            name='dataset_storage',
            field=models.TextField(null=True),
        ),
    ]
