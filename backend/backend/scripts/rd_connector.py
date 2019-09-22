import argparse
import requests
import webdav3.client as wc
import os

import tempfile


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Arguments to connect to resource drive')
    parser.add_argument('-username', type=str,
                        help='Username of Research Drive account', default=False)
    parser.add_argument('-password', type=str,
                        help='Password of Research Drive account', default=False)
    parser.add_argument('-download_file', type=str,
                        help='Filename you want to download', default=False)
    parser.add_argument('-list_files', type=bool,
                        help='Indexes the files in a list', default=False)
    parser.add_argument(
        '-token', type=str, help='Token to login instead of username and password', default=False)

    args = parser.parse_args()
    return args


def list_files(options):
    client = wc.Client(options)
    return client.list()


def download_file(options, filename, filepath=''):
    """
        Downloads file from research drive and store is in temporary file
    """

    client = wc.Client(options)

    download_location = os.path.join(os.getcwd(), filepath)

    if client.check(filename):
        client.download_sync(remote_path=filename,
                             local_path=download_location)
        print(f'File succesfully downloaded to: {download_location}')

    else:
        print('Could not locate the file')
        raise FileNotFoundError


def main():
    args = parse_arguments()

    options = {
        'webdav_hostname': "https://researchdrive.surfsara.nl",
        'webdav_root': '/remote.php/nonshib-webdav/'
    }

    if args.token:
        options['token'] = args.token
    else:
        options['webdav_login'] = args.username
        options['webdav_password'] = args.password

    if args.list_files == True:
        print(list_files(options))

    if args.download_file:
        print(download_file(options, args.download_file))

    return 0


if __name__ == "__main__":
    main()
