FROM python:3-alpine
WORKDIR /app
COPY requirements.txt .

# Install psycopg2 using apk to avoid having to build it.
RUN apk add py-psycopg2

RUN pip install -r requirements.txt
