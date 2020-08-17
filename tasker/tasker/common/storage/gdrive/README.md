# Google Drive support implementation

## Glossary

* SA - Service Account
* GCP - Google Cloud Platform

## Configuration

This is the manual for going down the SA route.

1. [Create service account](https://console.cloud.google.com/iam-admin/serviceaccounts);
2. Create a JSON key;
3. Provide good absolute path within `GDRIVE_CREDENTIALS_FILE` env variable;

### Integration tests

The tests expect two objects to be shared with the account identified by `GDRIVE_CREDENTIALS_FILE`: a file named `coronavirus_dataset.xlsx` and a folder `kitpes_master`.



## Known limitations

### Service account emails are ugly

It's: `[user-name]@[project-name].iam.gserviceaccount.com`. We can select nice `user-name` and `project-name`, though. 

What can be done to get rid of the ugly part: 
* Use custom domain (?)
* [Use regular Google account](https://stackoverflow.com/questions/23246671/using-regular-google-account-as-service-account)
