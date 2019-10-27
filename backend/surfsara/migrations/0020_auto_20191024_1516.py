# Generated by Django 2.2.6 on 2019-10-24 15:16

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('surfsara', '0019_auto_20191020_1338'),
    ]

    operations = [
        migrations.CreateModel(
            name='UserPermission',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('algorithm_provider', models.TextField()),
                ('dataset', models.TextField()),
                ('dataset_provider', models.TextField()),
                ('review_output', models.BooleanField(default=True)),
                ('registered_on', models.DateTimeField(auto_now_add=True)),
            ],
        ),
        migrations.AddField(
            model_name='permission',
            name='user_permission',
            field=models.BooleanField(default=True),
        ),
        migrations.AlterField(
            model_name='permission',
            name='algorithm',
            field=models.TextField(null=True),
        ),
    ]
