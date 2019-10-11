FROM python:3-alpine
WORKDIR /app
COPY requirements.txt .

RUN apk add curl libxslt postgresql-client

RUN apk add --virtual=build-deps -U postgresql-dev libxml2-dev libxslt-dev gcc musl-dev && \
    pip install -r requirements.txt && \
    apk del build-deps

COPY . ./
