# Generated by Django 2.2.6 on 2019-10-27 23:55

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0027_merge_20191027_2349")]

    operations = [
        migrations.RemoveField(model_name="permission", name="stream"),
        migrations.RemoveField(model_name="permission", name="user_permission"),
    ]