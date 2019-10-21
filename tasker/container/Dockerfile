# Docker file, which is used to build the base image
# for running secure containers.

FROM python
RUN apt-get update
RUN apt-get install -y strace
COPY ./scripts /app
RUN chmod u+x /app/tracerun.sh