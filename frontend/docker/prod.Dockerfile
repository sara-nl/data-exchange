FROM node:12-alpine AS build
WORKDIR /app

ENV NODE_ENV=production
COPY package.json yarn.lock ./

# Also install dev packages
RUN yarn install --production=false

COPY . ./
RUN yarn build

CMD node __sapper__/build
