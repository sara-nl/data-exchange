FROM openjdk:11-jre-stretch AS build
WORKDIR /app

RUN wget https://github.com/lihaoyi/mill/releases/download/0.7.3/0.7.3 \
    -O /usr/local/bin/mill && \
    chmod +x /usr/local/bin/mill    
RUN mill version
COPY build.sc .
COPY tasker ./tasker
COPY docker ./docker
RUN mill tasker.assembly