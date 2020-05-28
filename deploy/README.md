## Deploying the application

This document describes how the DataExchange aplication server can be automatically provisioned and configured for running the application. For more information see [this GitLab issue](https://git.ia.surfsara.nl/SOIL/secure-container/issues/48).

### Environment

The scripts have been tested with the following setup:

- HPC virual machine (1vCPU, 4GB RAM)
- Ubuntu-18.04-Server

### Preparation

- ☑ Install `ansible` on the [control node](https://docs.ansible.com/ansible/latest/user_guide/basic_concepts.html#control-node):
  - E.g.: `pipenv install && pipenv shell` inside of this directory. It's also OK to install ansible differently.
  - Make sure it's installed: `ansible --version`
- ☑ Ensure access to [managed nodes](https://docs.ansible.com/ansible/latest/user_guide/basic_concepts.html#managed-nodes):
  - Ensure that the hosts specified in `inventory.yml` are the ones you wan to use, or create your own inventory and specify it using `-i` flag;
  - Tests that the host from the inventory are reachable and accessible: `ansible -u ubuntu -i ./inventory.yml all -m ping`
- ☑ All managed nodes have user `ubuntu` with the sudo permission;
- ⚠️ Ensure there is a [Gitlab Deploy Token](https://git.ia.surfsara.nl/SOIL/secure-container/-/settings/repository) with the username `ansible` with permissions for `read_repository`, `read_registry` and you know its password;
- ⚠️ Ensure there is a correct FQDN hostname set in `inventory.yml` and `/etc/hostname` and `/etc/hosts`. It will be used for the TLS certificates generation.
- ⚠️ Ensure that `ALLOWED_HOSTS` in `settings.py` include the FQDN you want to use. Names ending with `.dataex-sara.surf-hosted.nl` are already allowed.

### Running Ansible

```shell
ansible-playbook -i inventory.yml -u ubuntu \
--extra-vars gitlab_username=<deploy_token_username> \
--extra-vars gitlab_password=<deploy_token_password>
--extra-vars RD_WEBDAV_USERNAME=<password> \
--extra-vars RD_WEBDAV_PASSWORD=<password> \
playbooks/dataexchange.yml
```

## Tips for writing Ansible scripts

- [Using tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) helps to save time.

### Known limitations

- Sometimes the playbook fails at the task "Log into Gitlab Docker Repository", but when executed again - succeeds 🤷‍♂️.
- The machine, where everything is being installed upon needs access to the git repo. This may become an issue in the future if we want to install the service on client's premises.
- Passwords have to be specified in cleartext (or using env variables) when running the ansible command. The downside is: they appear in cleartext in the log. A better soluton, perhaps, would be to use [Ansible Vault](https://docs.ansible.com/ansible/latest/user_guide/vault.html).
