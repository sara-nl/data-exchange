FROM alpine:3 AS build
WORKDIR /app

RUN apk add -U bash openjdk11-jre-headless curl unzip
RUN \
    wget https://github.com/lihaoyi/mill/releases/download/0.6.0/0.6.0 -O /usr/local/bin/mill && \
    chmod +x /usr/local/bin/mill    
RUN mill version
COPY build.sc .
COPY tasker ./tasker
COPY docker ./docker
RUN mill tasker.assembly