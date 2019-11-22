FROM node:12-alpine AS build
WORKDIR /app

ENV NODE_ENV=development
COPY package.json yarn.lock ./
RUN yarn install

CMD yarn dev