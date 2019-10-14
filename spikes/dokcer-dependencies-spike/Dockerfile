FROM python
ENV PYTHONDONTWRITEBYTECODE 1

RUN pip3 install pipenv

COPY requirements.txt requirements.txt
# COPY Pipfile Pipfile
# RUN pipenv install --deploy

RUN pip install -r requirements.txt
COPY . .
