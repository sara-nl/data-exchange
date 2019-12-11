# Backend

## Django commands
If you want to e.g. generate migrations, prefix the command with
`docker-compose run backend`. For example:

```bash
docker-compose run backend ./manage.py makemigrations
```

*Note that files generated in the container, such as migrations, will have
incorrect permissions. This is a side effect of how Docker works. You can fix
this with `sudo chown $USER backend/surfsara/migrations`*


## Solving installation problems


### Problem istalling `psycopg2`

```
export LDFLAGS="-I/usr/local/opt/openssl/include -L/usr/local/opt/openssl/lib"
```
