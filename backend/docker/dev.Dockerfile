FROM python:3-alpine
WORKDIR /app

RUN apk add -U curl libxslt postgresql-client postgresql-dev libxml2-dev \
                    libxslt-dev gcc musl-dev linux-headers

COPY requirements.txt .
RUN pip install -r requirements.txt
