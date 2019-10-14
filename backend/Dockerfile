FROM python:3-alpine
WORKDIR /app
COPY requirements.txt .

RUN apk add -U curl libxslt postgresql-client

RUN apk add --virtual=build-deps -U postgresql-dev libxml2-dev libxslt-dev gcc musl-dev linux-headers && \
    pip install -r requirements.txt && \
    pip install uwsgi && \
    apk del build-deps

COPY . ./
