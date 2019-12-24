#!/bin/bash

# This script pulls given version from Gitlab Container Registry and restarts 
# all services so that they use new images.

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

DOCKER_COMPOSE_VERSION_SUFFIX=":$CI_COMMIT_SHORT_SHA.$CI_COMMIT_REF_NAME" \
docker-compose -f docker-compose.yml -f docker-compose.prod.run.yml \
up -d