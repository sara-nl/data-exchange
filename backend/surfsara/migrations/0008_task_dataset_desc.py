# fmt: off
# Generated by Django 2.2.6 on 2019-10-03 13:47

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0007_auto_20191003_0849'),
    ]

    operations = [
        migrations.AddField(
            model_name='task',
            name='dataset_desc',
            field=models.TextField(default=''),
            preserve_default=False,
        ),
    ]
