FROM mkotsur/scala-mill:latest AS build
WORKDIR /app
COPY build.sc .
COPY tasker ./tasker
COPY docker ./docker
COPY .secrets ./.secrets
ENV GDRIVE_CREDENTIALS_FILE=/app/.secrets/gdrive-credentials.json
RUN mill tasker.assembly