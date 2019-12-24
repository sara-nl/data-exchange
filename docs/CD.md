# DataExchange CD pipeline
Currently, DataExchnge project implements the following:
* Every commit to `master` automatically goes to `staging` (a.k.a. `Test 2`).
* It's possible to update `production` (a.k.a. `Demo`) by running a single script.

⚠️ The version on `Demo` is usually a few development days behind `Test 2`.

## Glossary

* Continuous Integration (CI) and Continuous Deployment/Delivery (CD) - A set of modern DevOps practices for streamlining the release process. Make sure to check out [this article](https://www.atlassian.com/continuous-delivery/principles/continuous-integration-vs-delivery-vs-deployment) before reading this document further.
* GitLab Container Registry (CR) - a special service provided by GitLab, which provides a capability of storing Docker images. Docker images of the Data Exchange project can be found [here](https://git.ia.surfsara.nl/SOIL/secure-container/container_registry).
* Docker: Container, Image - see [Docker Overview](https://docs.docker.com/engine/docker-overview/).
* Build server – a role, which can be taken by a VM in the cloud or local development machine. In order to fulfill this role, a machine should be capable of: building Docker images, pushing them into the GitLab Container Registry.
* Application server – a role, which can be taken by a VM in the cloud or local development machine. A machine with this role should be able to fetch Docker images from the GitlabContainer Registry and start containers from this images.


## Image Versioning

Data Exchange follows this naming convention for images so that it's always simple to see which image corresponds to which service and version of the code.

* `repository` - Constant: `git.ia.surfsara.nl:5050/soil/secure-container`
* `service` - Name of the service. Corresponds to one of the `Dockerfile`s in our monorepo.
* `short-sha` – Git SHA, which shows the revision used for building this set of images.
* `branch-name` - Git branch name. Only images with `master` can be used for production deployments.

```
<repository>/<service-name>:<short-sha>.<branch-name>

//Examples:
git.ia.surfsara.nl:5050/soil/secure-container/tasker:438befd.43-deploy-to-dataexchange
git.ia.surfsara.nl:5050/soil/secure-container/backend:438befd.43-deploy-to-dataexchange
```

## Pre-reqisites for build and application servers

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



## Triggering pipeline locally

On build machine:
```
CI_COMMIT_SHORT_SHA=cee5658 CI_COMMIT_REF_NAME=43-deploy-to-dataexchange ./scripts/deploy-ci.sh
```

On application server (using the same `CI_COMMIT_SHORT_SHA` and `CI_COMMIT_REF_NAME`):

```
CI_COMMIT_SHORT_SHA=cee5658 CI_COMMIT_REF_NAME=43-deploy-to-dataexchange ./scripts/install-ci.sh
```

Obviously, you should replace `cee5658` and `43-deploy-to-dataexchange` with something suitable to your current situation!