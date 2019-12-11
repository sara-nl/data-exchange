FROM python:3-alpine
WORKDIR /app
COPY Pipfile .
COPY Pipfile.lock .

RUN apk add -U curl libxslt postgresql-client

RUN apk add --virtual=build-deps -U postgresql-dev libxml2-dev libxslt-dev gcc musl-dev linux-headers && \
    pip install pipenv \
    pipenv install --system \
    pip install uwsgi && \
    apk del build-deps

COPY . ./
