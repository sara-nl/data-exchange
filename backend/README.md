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


## PostgreSQL shell cheat sheet

* Starting psql within a running container: `docker exec -it data-exchange_postgres_1 psql -h postgres -U surfsara`
* List tables: `\dt`
* Describe table (e.g. `surfsara_user`): `\d surfsara_user;`
* Check new users: `SELECT email, date_joined FROM surfsara_user ORDER BY date_joined DESC;`

## Solving installation problems


### Problem installing `psycopg2`
* Install Postgres.app: `brew cask install postgres`
* `export PATH=$PATH:/Applications/Postgres.app/Contents/Versions/12/bin/`
* `export LDFLAGS="-I/usr/local/opt/openssl/include -L/usr/local/opt/openssl/lib`
