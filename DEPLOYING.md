# Deploying
## Frontend
First install `nodejs` and `npm`, and then use `npm` to install `yarn`:

```bash
sudo apt update
sudo apt install nodejs npm

sudo npm install -g yarn
```

Then, to install dependencies and build a production-ready version of the application, `cd` to the `frontend` directory and run:

```bash
# Install dependencies
yarn install

# Create a production-ready version
yarn build
```

After this is finished, run the frontend server. For now, this is done with `nohup`, but this will be replaced by Docker in the future:

```bash
nohup node __happer__/build &
```

Logs will be output to `nohup.out`.

## Backend
