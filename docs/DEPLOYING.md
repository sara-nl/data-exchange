# Deploying

First, install Docker and docker-compose, then run
`docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d`.

Then, configure a reverse proxy to `http://localhost:3000`
([instructions for nginx](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)),
set up HTTPS and you're done!
