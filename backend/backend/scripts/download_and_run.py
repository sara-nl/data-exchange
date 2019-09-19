from rd_connector import download_file
import argparse
import subprocess
import os
import sys
import stat
import tempfile
import signal


def download_and_run(alg_file, data_file, username, password, download_dir):
    """
        Downloads algorithm and data from research drive and runs them together.
        Output is stored in output.txt and files are removed.
    """

    alg_file_path = ""
    data_file_path = ""

    try:
        # download the files
        alg_file_path, data_file_path = create_files(download_dir)
        download_from_rd(alg_file, data_file,
                         username, password, alg_file_path, data_file_path)

        # change permissions of files
        os.chmod(alg_file_path, 0o555)
        os.chmod(data_file_path, 0o444)

        # run algorithm with data
        output_file = open(f"{download_dir}/output.txt", 'w')
        proc = subprocess.Popen(
            [f"python3 {alg_file_path} {data_file_path}"], shell=True, stdout=subprocess.PIPE)

        # Collect output and write to file
        output = proc.stdout.read().decode('utf-8')
        output_file.write(output)

        print("\nProgram Output:")
        print(output)

        print(f"Output file can be found in {os.getcwd()+download_dir}")

        # Delete algorithm and data files
        os.remove(alg_file_path)
        os.remove(data_file_path)

    except Exception as e:
        stop_running(alg_file_path, data_file_path)


def create_files(download_dir):
    """
        Creates temporary data and algorithm files with unique names
    """

    if not os.path.exists(download_dir):
        os.mkdir(download_dir)

    alg_file = tempfile.NamedTemporaryFile(
        suffix=".py", dir=download_dir, delete=False)

    data_file = tempfile.NamedTemporaryFile(
        suffix=".data", dir=download_dir, delete=False)

    return alg_file.name, data_file.name


def stop_running(alg_file_path, data_file_path):
    """
        Called if program stops runnings, deletes downloaded files
    """

    remove_files(alg_file_path, data_file_path)
    print("Error occured, files are removed")
    sys.exit(0)


def remove_files(alg_file_path, data_file_path):
    """
        Removes downloaded files
    """

    if alg_file_path:
        os.remove(alg_file_path)

    if data_file_path:
        os.remove(data_file_path)


def download_from_rd(alg_file, data_file, username, password, alg_file_path, data_file_path):
    """
        Downloads both the algorithm and data from researchdrive
    """

    options = {
        'webdav_hostname': "https://researchdrive.surfsara.nl",
        'webdav_root': '/remote.php/nonshib-webdav/',
        'webdav_login': username,
        'webdav_password': password
    }

    if alg_file and data_file:
        alg_file_path = download_file(
            options, filename=alg_file, filepath=alg_file_path)

        data_file_path = download_file(
            options, filename=data_file, filepath=data_file_path)

        if alg_file_path == "" or data_file_path == "":
            stop_running(alg_file_path, data_file_path)
    else:
        print("Need algorithm and data")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Arguments to connect to resource drive')
    parser.add_argument('-alg_file', type=str,
                        help='File with the algorithm', default='test_algorithm.py')
    parser.add_argument('-alg_data', type=str,
                        help='File with data', default='test_data.txt')
    parser.add_argument('-username', type=str,
                        help='Username of Research Drive account', default='tijs@wearebit.com')
    parser.add_argument('-password', type=str,
                        help='Password of Research Drive account', default='prototypingfutures')
    parser.add_argument('-download_dir', type=str,
                        help='Directory where downloaded folders are store', default='downloads')

    args = parser.parse_args()

    download_and_run(args.alg_file, args.alg_data,
                     args.username, args.password, args.download_dir)
