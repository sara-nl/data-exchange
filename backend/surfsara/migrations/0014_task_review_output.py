# Generated by Django 2.2.6 on 2019-10-11 23:24

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0013_auto_20191011_2128'),
    ]

    operations = [
        migrations.AddField(
            model_name='task',
            name='review_output',
            field=models.BooleanField(default=True, verbose_name=True),
            preserve_default=False,
        ),
    ]
