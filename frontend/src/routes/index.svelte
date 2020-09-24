<script lang="ts">
  import { onMount } from 'svelte'
  import { getShares, Share } from '../api/shares'
  import Spinner from '../components/Spinner.svelte'
  import SharesInfo from '../components/SharesInfo.svelte'

  import { mode, token } from '../stores'
  import { goto, stores } from '@sapper/app'
</script>

<svelte:head>
  <title>Data Exchange</title>
</svelte:head>

<h2 class="display-4 text-primary font-weigth-bold ym-5">DataExchange</h2>

<div class="container-fluid mx-auto">
  {#if Boolean($token)}
    <div class="row my-5">
      <SharesInfo />
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
            as on
            <a href="https://researchdrive.surfsara.nl">ResearchDrive</a>
            or
            <a href="https://drive.google.com/">Google Drive</a>
            .
          </p>
          <p>
            <b>2.</b>
            In ResearchDrive: click on the share icon next to the file and type
            in "Data Exchange" as users or groups to share with. Then select
            "Data Exchange" to share your file.
          </p>
          <p>
            <b>3.</b>
            In Google Drive: Right click on the file or folder, choose "Share"
            and enter this account id:
            <br />
            data-exchange-drive@quickstart-1584612842886.iam.gserviceaccount.com
            <br />
            into the "Share with people and groups" dialogue.
          </p>
          <p><b>4.</b> Your files should become visible on Data Exchange.</p>
        </div>
        <div class="row w-100 mt-4 mb-3">
          <button
            class="btn col-5 mx-3 py-2 btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`https://researchdrive.surfsara.nl`)}>
            <div class="px-4">Go to ResearchDrive</div>
          </button>
          <button
            class="btn col-4 mx-3 py-2 btn-primary rounded-xl font-weight-bold"
            on:click={() => goto(`/tasks/`)}>
            <div class="px-4">See your files</div>
          </button>
        </div>
      </div>
    </div>
  {:else}
    <div class="row my-5">
      <div class="col ym-5 p-5 bg-lightgrey rounded-xl">
        <h3 class="ym-3">How does the DataExchange work?</h3>
        <div class="col-8 font-weight-normal p-4">
          <p class="lead">
            The DataExchange offers a controlled and safe third-party
            environment where dataset and algorithm providers can safely
            cooperate and share data. The goal of DataExchange is to encourage
            sharing data while maintaining data sovereignty.
          </p>
          <p>
            On DataExchange, owners can share their datasets while staying in
            control. Algorithm owners can be given permissions to run on
            datasets, but will never be physically in control of it.
          </p>

          <h5 class="font-weight-bold">Secure container</h5>
          <p>
            The DataExchange provides a secure and networkless trusted
            third-party containers in which algorithm and dataset can safely
            interact. The dataset and algorithm are downloaded anonymously in
            the secure container and removed when the execution has been
            finished. Neither algorithm owner nor any other outside party will
            have physical hold of the dataset during this process.
          </p>
          <h5 class="font-weight-bold">Full data sovereignty</h5>

          <p>
            As owner of a dataset you can monitor all permissions, runs and
            review their output. All shared datasets are stored on your personal
            ResearchDrive and can be shared and unshared on your command at any
            time.
          </p>
        </div>
      </div>
    </div>
  {/if}
</div>
