# SURFsara Amsterdam Data Exchange
This is the repository for the SURFsara Amsterdam Data Exchange project.

*These instructions are for Ubuntu and similar distributions
(Debian, Mint, etc). The package management commands will be different on other
distributions or macOS, but the gist will remain the same.*

# Developing
Install docker and docker-compose (when you are using an old Ubuntu version, it
might be worth considering getting `docker-compose` from Pip instead of apt).

Then, run everything with `docker-compose up --build`. This will build all
dependencies and then run everything with hot reloading.

## Django commands
If you want to e.g. generate migrations, prefix the command with
`docker-compose run backend`. For example:

```bash
docker-compose run backend ./manage.py makemigrations
```

*Note that files generated in the container, such as migrations, will have
incorrect permissions. This is a side effect of how Docker works. You can fix
this with `sudo chown $USER backend/surfsara/migrations`*

# Mail
All outgoing emails can be read at: https://dataexchange.surfsara.nl/mail

