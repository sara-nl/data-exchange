# SURFsara Amsterdam Data Exchange
This is the repository for the SURFsara Amsterdam Data Exchange project.

*These instructions are for Ubuntu and similar distributions (Debian, Mint, etc). The package management commands will be different on other distributions or macOS, but the gist will remain the same.*

# Developing
Currently, deployment is done manually. This will be replaced by Docker in the near future.

## Front-end
First install `nodejs` and `npm`, and then use `npm` to install `yarn`:

```bash
sudo apt update
sudo apt install nodejs npm

sudo npm install -g yarn
```

Then, to install dependencies and run a development server, `cd` to the `frontend` directory and run:

```bash
# Install dependencies
yarn install

# Run frontend development server
yarn dev
```

Then open up [localhost:3000](http://localhost:3000) in your browser.


## Back-end

### Dependencies
First, install `python3` (if not installed yet) and `virtualenv`:

```bash
sudo apt update
sudo apt install python3 python3-virtualenv
```

**🍏 Extra steps for OSX:**
```
brew install postgresql gsl openssl
export LDFLAGS="-L/usr/local/opt/openssl/lib"
export CPPFLAGS="-I/usr/local/opt/openssl/include"
```

Then, to install the dependencies, first create a virtualenv, activate it and install them with `pip`:

```bash
# Install virtualenv if you have not yet installed it.
pip install --user virtualenv

# Create a virtualenv and activate it.
python3 -m virtualenv venv
source venv/bin/activate

# Install the requirements.
pip install -r requirements.txt

# Install docker
apt-get install docker.io

# Add docker
sudo groupadd docker
sudo usermod -aG docker $USER
```

### Running
To run the backend, activate the virtualenv, run migrations and run the server:

```bash
# Activate the virtualenv
source venv/bin/activate

# Run migrations
python manage.py migrate

# Run the server
python manage.py runserver
```

Then, open another terminal (or use `screen`/`tmux`/similar) and run the task response listener:

```bash
python manage.py listen
```

### Mail

All outgoing emails can be read at: https://dataexchange.surfsara.nl/mail


## Tasker
For instructions on how to run the tasker, see `tasker/README.md`.
