#!/bin/bash

NEW_IMAGE_URL="git.ia.surfsara.nl:5050/soil/secure-container/datex"
NEW_IMAGE_TAG="${NEW_IMAGE_URL}:$(git rev-parse --short HEAD)"
LATEST_IMAGE_TAG="${NEW_IMAGE_URL}:latest"

docker build . -t $NEW_IMAGE_TAG
docker tag $NEW_IMAGE_TAG $LATEST_IMAGE_TAG

docker push "$NEW_IMAGE_TAG"
docker push "$LATEST_IMAGE_TAG"

echo "Created a new image. You can use either $NEW_IMAGE_TAG or $LATEST_IMAGE_TAG"
