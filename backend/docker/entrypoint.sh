#!/usr/bin/env ash
until curl -s -o /dev/null -u $RABBITMQ_USERNAME:$RABBITMQ_PASSWORD http://$RABBITMQ_HOST:15672/api/overview; do
    echo >&2 "Waiting for RabbitMQ..."
    sleep 1
done

until psql -h "$DJANGO_DB_HOST" -U "$DJANGO_DB_USER" -c '\q'; do
  echo >&2 "Waiting for PostgreSQL..."
  sleep 1
done

if [[ "$DJANGO_DEBUG" -eq 0 && "$1" == "runserver" ]]; then
  python -u manage.py migrate
  python -u manage.py collectstatic

  exec uwsgi --ini uwsgi.ini
else
  if [[ "$1" == "runserver" ]]; then
    python -u manage.py migrate
  fi

  exec python -u manage.py $*
fi
