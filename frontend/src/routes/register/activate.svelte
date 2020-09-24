<script>
  import { onMount } from 'svelte'
  import { stores } from '@sapper/app'

  import Users, { ActivateRequest } from '../../api/users'
  import Spinner from '../../components/Spinner.svelte'

  const { page } = stores()
  const { token, pk } = $page.query

  let loading = true
  let error = null

  onMount(async () => {
    const requestData = new ActivateRequest(token)
    const { data } = await Users.activate(pk, requestData)
    if (!data.valid) {
      error = data.error
    }

    loading = false
  })
</script>

<div class="container-fluid mx-auto">
  <div class="row">
    <div class="col-xs-12 col-md-8">
      {#if loading}
        <Spinner />
      {:else if error}
        <h1>Something went wrong</h1>
        <p>{error}</p>
      {:else}
        <h1>Account activated!</h1>

        <p>
          Your account has been activated successfully. You can now continue to
          the log-in page to log into your new account.
        </p>

        <p><a href="/login">Go to login page</a></p>
      {/if}
    </div>
  </div>
</div>
