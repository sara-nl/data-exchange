# Docker file, which is used to build the base image
# for running secure containers.

FROM python:3.7
RUN apt-get update
RUN apt-get install -y strace
RUN pip install --upgrade pip
COPY ./scripts /app
RUN chmod u+x /app/tracerun.sh