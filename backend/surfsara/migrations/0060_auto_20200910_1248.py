# Generated by Django 2.2.16 on 2020-09-10 12:48

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0059_auto_20200820_0912'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='user',
            name='webdav_password',
        ),
        migrations.RemoveField(
            model_name='user',
            name='webdav_username',
        ),
    ]
