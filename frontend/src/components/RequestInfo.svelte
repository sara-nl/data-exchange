<script lang="ts">
  import AlgorithmReport from './AlgorithmReport.svelte'
  import AlgorithmSourceCode from './AlgorithmSourceCode.svelte'
  import Spinner from './Spinner.svelte'
  import { Permission } from '../api/permissions'
  import { UserRole } from '../api/users'
  import { mode, token, email } from '../stores'
  import { onMount } from 'svelte'
  import dayjs from 'dayjs'
  import relativeTime from 'dayjs/plugin/relativeTime'
  dayjs.extend(relativeTime)

  import * as jq from 'jquery'

  // @ts-ignore
  export let permission: Permission

  let currentMode: UserRole = 'algorithm'

  email.subscribe(e => {
    if (
      permission.dataset_provider === e &&
      permission.algorithm_provider === e
    ) {
      // Let the user decide
      mode.subscribe(m => (currentMode = m || currentMode))
    } else if (permission.dataset_provider === e) currentMode = 'data'
    else currentMode = 'algorithm'
  })

  const reportAvailable = Boolean(permission.algorithm_report)

  jq(() => {
    jq('[data-toggle="tooltip"]').tooltip()
  })
</script>

<div class="jumbotron p-4">
  <p class="lead">
    Request for a &nbsp;
    <svg
      width="1em"
      height="1em"
      viewBox="0 0 16 16"
      class="bi bi-key"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg">
      <path
        fill-rule="evenodd"
        d="M0 8a4 4 0 0 1 7.465-2H14a.5.5 0 0 1 .354.146l1.5 1.5a.5.5 0 0 1 0
        .708l-1.5 1.5a.5.5 0 0 1-.708 0L13 9.207l-.646.647a.5.5 0 0 1-.708 0L11
        9.207l-.646.647a.5.5 0 0 1-.708 0L9 9.207l-.646.647A.5.5 0 0 1 8
        10h-.535A4 4 0 0 1 0 8zm4-3a3 3 0 1 0 2.712 4.285A.5.5 0 0 1 7.163
        9h.63l.853-.854a.5.5 0 0 1 .708 0l.646.647.646-.647a.5.5 0 0 1 .708
        0l.646.647.646-.647a.5.5 0 0 1 .708 0l.646.647.793-.793-1-1h-6.63a.5.5 0
        0 1-.451-.285A3 3 0 0 0 4 5z" />
      <path d="M4 8a1 1 0 1 1-2 0 1 1 0 0 1 2 0z" />
    </svg>
    <u>{permission.permission_type}</u>
  </p>
  <blockquote class="blockquote text-center">
    <p>{permission.request_description}</p>
  </blockquote>
  <hr class="my-4" />
  <div class="container-fluid">
    <div class="row">
      <div class="col-sm">
        <small
          data-toggle="tooltip"
          title={dayjs(permission.registered_on).format('DD-MM-YYYY HH:mm')}>
          <svg
            width="1em"
            height="1em"
            viewBox="0 0 16 16"
            class="bi bi-calendar-event"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg">
            <path
              fill-rule="evenodd"
              d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2
              2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1
              .5-.5zM1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1z" />
            <path
              d="M11 6.5a.5.5 0 0 1 .5-.5h1a.5.5 0 0 1 .5.5v1a.5.5 0 0
              1-.5.5h-1a.5.5 0 0 1-.5-.5v-1z" />
          </svg>
          Submitted {dayjs(permission.registered_on).fromNow()}
        </small>
      </div>
      <div class="col-sm">
        <small>
          <svg
            width="1em"
            height="1em"
            viewBox="0 0 16 16"
            class="bi bi-person-circle"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg">
            <path
              d="M13.468 12.37C12.758 11.226 11.195 10 8 10s-4.757 1.225-5.468
              2.37A6.987 6.987 0 0 0 8 15a6.987 6.987 0 0 0 5.468-2.63z" />
            <path fill-rule="evenodd" d="M8 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
            <path
              fill-rule="evenodd"
              d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zM0 8a8 8 0 1 1 16 0A8 8 0 0 1
              0 8z" />
          </svg>
          By {permission.algorithm_provider}
        </small>
      </div>
      <div class="col-sm">
        <small>
          <svg
            width="1em"
            height="1em"
            viewBox="0 0 16 16"
            class="bi bi-flag"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg">
            <path
              fill-rule="evenodd"
              d="M14.778.085A.5.5 0 0 1 15 .5V8a.5.5 0 0 1-.314.464L14.5
              8l.186.464-.003.001-.006.003-.023.009a12.435 12.435 0 0
              1-.397.15c-.264.095-.631.223-1.047.35-.816.252-1.879.523-2.71.523-.847
              0-1.548-.28-2.158-.525l-.028-.01C7.68 8.71 7.14 8.5 6.5 8.5c-.7
              0-1.638.23-2.437.477A19.626 19.626 0 0 0 3 9.342V15.5a.5.5 0 0 1-1
              0V.5a.5.5 0 0 1 1 0v.282c.226-.079.496-.17.79-.26C4.606.272 5.67 0
              6.5 0c.84 0 1.524.277 2.121.519l.043.018C9.286.788 9.828 1 10.5
              1c.7 0 1.638-.23 2.437-.477a19.587 19.587 0 0 0
              1.349-.476l.019-.007.004-.002h.001M14
              1.221c-.22.078-.48.167-.766.255-.81.252-1.872.523-2.734.523-.886
              0-1.592-.286-2.203-.534l-.008-.003C7.662 1.21 7.139 1 6.5 1c-.669
              0-1.606.229-2.415.478A21.294 21.294 0 0 0 3
              1.845v6.433c.22-.078.48-.167.766-.255C4.576 7.77 5.638 7.5 6.5
              7.5c.847 0 1.548.28 2.158.525l.028.01C9.32 8.29 9.86 8.5 10.5
              8.5c.668 0 1.606-.229 2.415-.478A21.317 21.317 0 0 0 14
              7.655V1.222z" />
          </svg>
          State {permission.state}
        </small>
      </div>
    </div>
  </div>

</div>

<div class="row mx-auto">
  <div class="col-sm-4 h-50">
    <b>Algorithm name:</b>
    <pre>{permission.algorithm}</pre>
  </div>
  <div class="col-sm-4 h-50">
    {#if reportAvailable}
      <b>Algorithm hash:</b>
      <pre>{permission.algorithm_etag}</pre>
    {:else}
      <Spinner small />
    {/if}
  </div>
  <div class="col-sm-4 h-50">
    <b>Libraries:</b>
    {#each permission.algorithm_report.imports as dependency}
      &nbsp;
      <span class="badge badge-primary">{dependency}</span>
    {/each}
  </div>
</div>

{#if reportAvailable}
  <hr />
  Lines: {permission.algorithm_report.lines}, Words: {permission.algorithm_report.words},
  Characters: {permission.algorithm_report.chars}
  <AlgorithmSourceCode files={permission.algorithm_report.contents} />
{:else}
  <div class="row mx-auto">
    <Spinner small />
  </div>
{/if}

<hr />
