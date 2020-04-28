#!/bin/bash

# This script builds and runs the development version of all services

docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d $1