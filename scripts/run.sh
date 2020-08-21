#!/bin/bash

# This script pulls given version from Gitlab Container Registry and restarts 
# all services so that they use new images.

# Attention! This script should be executed from Gitlab CI environment only.
# It requires $DOCKER_IMAGE_TAG variable to be set!

if [[ -z "$DOCKER_IMAGE_TAG" ]]; then
    echo "Must provide DOCKER_IMAGE_TAG in environment" 1>&2
    exit 1
fi

docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
up -d