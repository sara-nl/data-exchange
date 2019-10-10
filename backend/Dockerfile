FROM python:3-alpine
WORKDIR /app
COPY requirements.txt .


# Install psycopg2 using apk to avoid having to build it.
RUN apk add py-psycopg2

RUN apk add --virtual=build-deps -U libxml2-dev libxslt-dev gcc musl-dev && \
    pip install -r requirements.txt && \
    apk del build-deps

COPY . ./
