FROM alpine:3 AS build
WORKDIR /app

RUN apk add -U bash openjdk11-jre-headless curl
RUN \
    wget https://github.com/lihaoyi/mill/releases/download/0.5.2/0.5.2 -O /usr/local/bin/mill && \
    chmod +x /usr/local/bin/mill

COPY build.sc .
RUN mill tasker.compile
COPY tasker ./tasker
RUN mill tasker.assembly

FROM alpine:3
WORKDIR /app

RUN apk add -U openjdk11-jre-headless unzip curl
COPY docker docker
COPY --from=build /app/out/tasker/assembly/dest/out.jar out.jar
