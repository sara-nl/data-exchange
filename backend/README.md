# SURFsara Backend

To install the dependencies, first create a virtualenv, activate it and install them with `pip`:

```bash
# Install virtualenv if you have not yet installed it.
pip install --user virtualenv

# Create a virtualenv and activate it.
python3 -m virtualenv venv
source venv/bin/activate

# Install the requirements.
pip install -r requirements.txt
```

To run the backend, activate the virtualenv and run the server:

```bash
# Activate the virtualenv
source venv/bin/activate

# Run the server
python manage.py runserver
```

This will soon be replaced with docker, once the ontwikkelstraat is finished.
