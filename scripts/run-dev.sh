#!/bin/bash

# This script builds and runs development version of all services

docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d