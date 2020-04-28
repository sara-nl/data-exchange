#!/usr/bin/env sh
until curl -s -o /dev/null -u guest:guest http://$RABBITMQ_HOST:15672/api/overview; do
    echo "Waiting for RabbitMQ..."
    sleep 1
done

echo "LOG_LEVEL=$LOG_LEVEL"

exec java -Djava.io.tmpdir=/tmp/tasker \
-DrunnerLogLevel=$LOG_LEVEL \
-DwatcherLogLevel=$LOG_LEVEL \
-DcacherLogLevel=$LOG_LEVEL \
-jar /app/out/tasker/assembly/dest/out.jar
