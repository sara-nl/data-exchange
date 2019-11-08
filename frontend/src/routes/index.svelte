<script lang="ts">
  import { onMount } from "svelte";
  import LoadFiles from "../api/loader";
  import Spinner from "../components/Spinner.svelte";
  import { mode } from "../stores";
  import { goto, stores } from "@sapper/app";

  let own_algorithms: any = null;
  let own_datasets: any = null;

  onMount(async () => {
    await getUserFiles();
  });

  async function getUserFiles() {
    try {
      let { data: response } = await LoadFiles.start();
      own_algorithms = response.output.own_algorithms;
      own_datasets = response.output.own_datasets;
    } catch (error) {
      console.log(error.toString());
    }
  }
</script>

<svelte:head>
  <title>DEX</title>
</svelte:head>

<h2 class="display-4 text-primary font-weigth-bold ym-5">DataExchange</h2>
<h2>
  <small class="text-muted">a SURFsara x Bit concept</small>
</h2>

<div class="container-fluid">
  <div class="row my-5">
    <div class="col my-3 p-4 px-5 bg-lightgrey rounded-xl">
      <h3>Where to start?</h3>
      {#if own_datasets === null || own_algorithms === null}
        <Spinner />
      {:else if (own_datasets.length > 0) & ($mode === 'data')}
        <div class="my-3">
          <p>
            You have shared datasets with the DataExchange
            <br />
            Click here to see any requests made for your data:
          </p>
          <button
            class="btn btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`/requests`)}>

            <div class="px-4">Go to your requests</div>
          </button>
        </div>
      {:else if (own_algorithms.length > 0) & ($mode === 'algorithm')}
        <div class="my-3">
          <p>
            You have shared algorithms with the DataExchange
            <br />
            Click here to make a request for the use of a dataset:
          </p>
          <button
            class="btn btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`/tasks/request`)}>

            <div class="px-4">Make a request</div>
          </button>
        </div>
      {:else}
        <div class="my-3">
          <p>You haven't shared any files with the DataExchange</p>
        </div>
      {/if}
    </div>
    <div class="col-1" />
    <div class="col bg-lightgrey my-3 p-4 px-5">
      <h3>
        How to share files
        <small class="text-muted">with the DataExchange</small>
      </h3>
      <div class="my-3">
        <p>
          <b>1.</b>
          Register and activate account with the
          <u>same email</u>
          as on ResearchDrive
        </p>
        <p>
          <b>2.</b>
          In
          <a href="https://researchdrive.surfsara.nl">ResearchDrive</a>
          click on the share icon next to the file
        </p>
        <p>
          <b>3.</b>
          Type in "Data Exchange" as users or groups to share with
        </p>
        <p>
          <b>4.</b>
          Select "Data Exchange" to share your file
        </p>
        <p>
          <b>5.</b>
          See your files on the website.
        </p>

      </div>
      <div class="row w-100 mt-4 mb-3">
        <div class="col-6">
          <button
            class="btn btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`https://researchdrive.surfsara.nl`)}>

            <div class="px-4">Go to ResearchDrive</div>
          </button>
        </div>
        <div class="col-5">
          <button
            class="btn btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`/tasks/`)}>

            <div class="px-4">See your files</div>
          </button>
        </div>
      </div>
    </div>
  </div>
  <div class="row my-5">
    <div class="col ym-5 p-5 bg-lightgrey rounded-xl">
      <h3 class="ym-3">How does the DataExchange work?</h3>
      <div class="col-8 font-weight-normal p-4">
        <p class="lead">
          The DataExchange offers a controlled and safe third-party enviroment
          where dataset and algorithm providers can safely cooperate and share
          data. The goal of DataExchange to encourage sharing data while
          mainting data sovereignty.
        </p>
        <p>
          On DataExchange owners of datasets can share their datasets while
          staying in control over their data. Algorithm owners can be given
          permissions to run on datasets, but will never be physically in
          control of it.
        </p>

        <h5 class="font-weight-bold">Secure container</h5>
        <p>
          Instead the DataExchange provides secure and networkless third-party
          containers in which algorithm and dataset can safely interact. 
          The dataset and algorithm are anonymously downloaded in the secure container when running and removed when running is finished.
          Neither
          algorithm owner nor any other outside party will have physical hold of the dataset during this process. 

        </p>
          <h5 class="font-weight-bold">Full data sovereignty</h5>

        <p>
           As owner of a dataset
          you can monitor all permissions, runs and review their output.  
          All shared datasets are stored on your personal ResearchDrive and can be shared and unshared at any moment you want. 
        </p>

      </div>

    </div>
  </div>
</div>
