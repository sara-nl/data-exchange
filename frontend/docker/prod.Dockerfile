FROM node:12-alpine AS build
WORKDIR /app

ENV NODE_ENV=production
COPY package.json yarn.lock ./
RUN yarn install

COPY . ./
RUN yarn build
