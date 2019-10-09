#!/usr/bin/env ash
until curl -s -o /dev/null -u guest:guest http://$RABBITMQ_HOST:15672/api/overview; do
    echo "Waiting for RabbitMQ..."
    sleep 1
done

java -jar out.jar
