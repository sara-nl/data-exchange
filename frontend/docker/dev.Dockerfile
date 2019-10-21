FROM node:12-alpine AS build
WORKDIR /app

COPY package.json yarn.lock ./
RUN yarn install
