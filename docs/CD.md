# DataExchange CD pipeline
Currently, DataExchnge project implements the following:
* Every commit to `master` automatically goes to `staging` (a.k.a. `Test 2`).
* It's possible to update `production` (a.k.a. `Demo`) by running a single script.

⚠️ The version on `Demo` is usually a few development days/weeks behind `Test 2`.

## Glossary

* Continuous Integration (CI) and Continuous Deployment/Delivery (CD) - A set of modern DevOps practices for streamlining the release process. Make sure to check out [this article](https://www.atlassian.com/continuous-delivery/principles/continuous-integration-vs-delivery-vs-deployment) before reading this document further.
* GitLab Container Registry (CR) - a special service provided by GitLab, which provides a capability of storing Docker images. Docker images of the Data Exchange project can be found [here](https://git.ia.surfsara.nl/SOIL/secure-container/container_registry).
* Docker: Container, Image - see [Docker Overview](https://docs.docker.com/engine/docker-overview/).
* Build server – a role, which can be taken by a VM in the cloud or local development machine. In order to fulfill this role, a machine should be capable of: building Docker images, pushing them into the GitLab Container Registry.
* Application server – a role, which can be taken by a VM in the cloud or local development machine. A machine with this role should be able to fetch Docker images from the GitlabContainer Registry and start containers from this images.
* Gitlab CI/CD - a tool built into GitLab for software development through the continuous methodologies metioned above.


## Setting up Gitlab CI/CD for this project

GitLab CI/CD is configured by a file called .gitlab-ci.yml placed at the repository’s root. The scripts set in this file are executed by the GitLab Runner. Typically those scipts execute actions upon the source code from the git repository.

Both: the scripts and the source code depend upon environment variables stored in the configuration section "Vaiables" of [Gitlab CI/CD settings](https://git.ia.surfsara.nl/SOIL/secure-container/-/settings/ci_cd). At the moment the following variables must be configured there:
* `RD_WEBDAV_PASSWORD`
* `RD_WEBDAV_USERNAME`
* `TEST2_SSH_ADDRESS`
* `TEST2_SSH_KEY`


## Image Versioning

Data Exchange follows this naming convention for images so that it's always simple to see which image corresponds to which service and version of the code.

* `repository` - Constant: `git.ia.surfsara.nl:5050/soil/secure-container`
* `service` - Name of the service. Corresponds to one of the `Dockerfile`s in our monorepo.
* `short-sha` – Git SHA, which shows the revision used for building this set of images.
* `branch-name` - Git branch name.

```
<repository>/<service-name>/<branch-name>:<short-sha>

//Examples:
git.ia.surfsara.nl:5050/soil/secure-container/tasker/43-deploy-to-dataexchange:438befd
git.ia.surfsara.nl:5050/soil/secure-container/backend/43-deploy-to-dataexchange:438befd
```

## Pre-reqisites for the build and application servers

### Authenticate at Gitlab CR

```
docker login git.ia.surfsara.nl:5050
```

Follow [GitLab documentation](https://docs.gitlab.com/ee/user/packages/container_registry/#build-and-push-images) for obtaining credentials with the right level of permissions. 

Build servers need [Personal Access Token](https://git.ia.surfsara.nl/help/user/profile/personal_access_tokens.md).

Application servers should be fine with [Deploy Token](https://docs.gitlab.com/ee/user/project/deploy_tokens/index.html).

⚠️ This situation is sub-optimal. Ideally Deploy Tokens (configured per-project) should be used in Build servers as well, but because of weird permission configuration of GitLab it doesn't seem possible to use Deploy Tokens for pushing images into CR.

### Docker and Docker Compose installed

* [Installing Docker](https://docs.docker.com/install/)
* [Installing Docker Compose](https://docs.docker.com/compose/install/)

## Deploying the latest version of the app

Assuming the containers are available in the [Gitlab Container Registry](https://git.ia.surfsara.nl/SOIL/secure-container/container_registry):

```
DOCKER_IMAGE_TAG=cee5658 DOCKER_IMAGE_REF_NAME=43-deploy-to-dataexchange ./scripts/run.sh
```

Obviously, you should replace `cee5658` and `43-deploy-to-dataexchange` with something suitable to your current situation!

## Cleanup

Unfortunately, there is no straightforward way to configure cleanup in Gitlab CR. It needs to be done manually according to the following rules:
* `<repository>/<service-name>/<branch-name>` can be deleted when the git branch <branch-name> is deleted;
* `<repository>/<service-name>/<tag>` all tags besides "latest" (and perhaps a few most recent ones) can be deleted.