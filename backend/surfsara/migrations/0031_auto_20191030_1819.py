# Generated by Django 2.2.6 on 2019-10-30 18:19

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [("surfsara", "0030_auto_20191030_1744")]

    operations = [
        migrations.AlterField(
            model_name="permission",
            name="permission_type",
            field=models.CharField(
                choices=[
                    ("one time permission", "one time permission"),
                    ("user permission", "user permission"),
                    ("stream permission", "stream permission"),
                    ("no permission", "no permission"),
                ],
                default="no permission",
                max_length=255,
            ),
        )
    ]
