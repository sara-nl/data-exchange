# Generated by Django 2.2.7 on 2019-11-28 13:55

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("surfsara", "0046_auto_20191128_1351"),
    ]

    operations = [
        migrations.AlterField(
            model_name="request",
            name="requester_email",
            field=models.EmailField(max_length=254),
        ),
    ]
