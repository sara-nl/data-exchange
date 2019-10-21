#!/bin/bash

NEW_IMAGE_TAG="datex:$(git rev-parse --short HEAD)"
LATEST_IMAGE_TAG="datex:latest"

docker build . -t $NEW_IMAGE_TAG
docker tag $NEW_IMAGE_TAG $LATEST_IMAGE_TAG

echo "Created a new image. You can use either $NEW_IMAGE_TAG or $LATEST_IMAGE_TAG"