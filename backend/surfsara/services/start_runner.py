
def start_runner(algorithm_file, data_file):
    runner = RunContainer(
        remote_algorithm_path=request.data["algorithm_file"],
        remote_data_path=request.data["data_file"],
        download_dir="./files",
    )

    try:
        runner.download_files()
        file  = runner.run_algorithm()
        with open(file, "r") as f:
            output = f.read()
    except Exception as error:
        print(error)
        output = "Could not run with selected files.\nPlease refresh and try again."

    return output