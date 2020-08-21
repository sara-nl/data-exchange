FROM mkotsur/scala-mill:latest AS build
WORKDIR /app
COPY build.sc .
COPY tasker ./tasker
RUN mill tasker.assembly


FROM openjdk:14
WORKDIR /app
COPY --from=build /app/out/tasker/assembly/dest/out.jar /app/dexter.jar
ENV GDRIVE_CREDENTIALS_FILE=/app/.secrets/gdrive-credentials.json
COPY docker ./docker
COPY .secrets ./.secrets
CMD ./docker/entrypoint.sh