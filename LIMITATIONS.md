# Known limitations

This is the list of known ~issues~ limitations.

## Product limitations
* Algorithm is Python only;
* Research Drive only;
* Output is only STDOUT/STDERR;
* Webdav interface of Research Drive is relatively slow;
* A shared algorithm is file or a folder with one layer depth
  (so a folder containing only files. If it contains more folders, they will not be used/shown)
* Changes to code or data files are detected, but no diff interface was implemented
* Algorithm folder can contain auxiliary files, including binary files (e.g. images or compiled code), but binary files are not shown for review.

## Known security issues
* Edge case; if:
  - Algorithm A is uploaded.
  - Data owner opens page on algorithm A.
  - While the data owner is reviewing, algorithm B gets uploaded.
  - Data owner *thinks* they're approving A, but they are actually approving B.

  The ETag should be checked for changes when a data owner approves an
  algorithm.

## File sharing
* Maximal level of nested directories in code/data folders is 50;
* Ignores empty folders in code/data;
* When a file is shared and later renamed, DataExchange will still see it with
  the old name. Workaround: unshare/share again.


## Deployment

* Because we need to manage a docker container from a docker container,
  matching the paths of mounted volumes becomes non-trivial. There is a
  `/tmp/tasker` path hardcoded in the Docker compose file and `entrypoint.sh`
  of tasker. This can be simplified by implementing *copying* of files from
  `tasker` container into secure container, instead of mounting.
