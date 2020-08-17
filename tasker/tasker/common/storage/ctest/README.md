# Cloud tests

This document contains the instructions of setting up automated integrations tests against cloud storage providers. In `resources/shares.tar.gz` you'll find a set of files and folders that need to be uploaded into the cloud provider and shared with the account you want to run tests with. 

## Step 1: Setting up credentials

You'll need a *pair* of accounts for each storage provider. The first will represent the data owner's account, Dexter won't need to access this one. The second will represent the DataExchange service account and Dexter will need to be access it. You'll have to upload the files to Account 1 and share them with Account 2.

### When
When setting up a new development environment

### What:

1.1 Put Google Drive service account credentials for Account 2 `.secrets/gdrive-credentials.json` in the same directory where `build.sc` lives, which is the root of Dexter;
2.2 Ensure the process which runs tests (IDE, shell, etc.) has access to `RD_WEBDAV_USERNAME_TEST` and `RD_WEBDAV_PASSWORD_TEST`. Those credentials should provide WebDav access to Account 2 of the Research Drive.  

## Step 2: Uploading and sharing the files

### When: 

a) when migrating to a different account(s) for existing cloud providers (e.g. when changing `RD_WEBDAV_PASSWORD_TEST` or `.secrets/gdrive-credentials.json` file).

b) when covering a new storage provider with tests (good idea to do so!). 

### What:

1. Unarchive `fixtures/shares.tar.gz`;
2. Upload the files into any location of Account 1;
3. Share each file/folder *separately* with Account 2;
4. Run the tests, they should be fine now.

## What happens if I don't upload the files?

The tests will assume those files to be present and will fail false-positively if the files won't be uploaded or shared correctly.

## Any questions about setting up the tests?

Ask Mike Kotsur <mike.kotsur@surf.nl> or Freek Dijkstra <freek.dijkstra@surf.nl> 