# Docker file, which is used to build the base image
# for running secure containers.

FROM python
COPY . /app
RUN apt-get update
RUN apt-get install -y strace nodejs npm jq
RUN npm i -g b3-strace-parser