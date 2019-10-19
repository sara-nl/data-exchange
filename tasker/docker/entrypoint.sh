#!/usr/bin/env ash
until curl -s -o /dev/null -u guest:guest http://$RABBITMQ_HOST:15672/api/overview; do
    echo "Waiting for RabbitMQ..."
    sleep 1
done

if [[ "$ENVIRONMENT" == "development" ]]; then
    mill --watch tasker.runBackground
    exec mill --watch tasker.compile
else
    exec java -Djava.io.tmpdir=/tmp/tasker -jar out.jar
fi
