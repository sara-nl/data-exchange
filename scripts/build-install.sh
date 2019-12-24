#!/bin/bash
docker-compose -f docker-compose.yml -f docker-compose.prod.build.yml up --build -d
