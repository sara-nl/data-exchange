#!/bin/bash

# This script builds images of given services (or all known if not specified),
# tags those images using git branch name and revision and pushes the images
# into Gitlab Container Registry.

# Attention! This script should be executed from Gitlab CI environment only.
# It requires $CI_COMMIT_REF_NAME and $CI_COMMIT_SHORT_SHA variables
# to be set!

if [[ -z "$CI_COMMIT_REF_NAME" ]]; then
    echo "Must provide CI_COMMIT_REF_NAME in environment" 1>&2
    exit 1
fi


if [[ -z "$CI_COMMIT_SHORT_SHA" ]]; then
    echo "Must provide CI_COMMIT_SHORT_SHA in environment" 1>&2
    exit 1
fi

ALL_SERVICES="tasker backend backend_listen frontend"

if [[ ! -z "$1" ]]; then
    ALL_SERVICES="$1"
fi

echo "Building images of ${ALL_SERVICES}"

DOCKER_COMPOSE_VERSION_SUFFIX=":$CI_COMMIT_SHORT_SHA.$CI_COMMIT_REF_NAME" \
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build ${ALL_SERVICES}

DOCKER_COMPOSE_VERSION_SUFFIX=":$CI_COMMIT_SHORT_SHA.$CI_COMMIT_REF_NAME" \
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
push ${ALL_SERVICES}
