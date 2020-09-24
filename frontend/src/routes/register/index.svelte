<script lang="ts">
  import { goto } from '@sapper/app'
  import Users, { RegisterRequest } from '../../api/users'

  let data = new RegisterRequest('', '')
  let passwordRepeat: any = null

  let loading = false

  async function submit(event: any) {
    event.preventDefault()

    const passwordValid = data.password === passwordRepeat.value
    if (!passwordValid) {
      passwordRepeat.setCustomValidity("Passwords don't match")
      return
    } else {
      passwordRepeat.setCustomValidity('')
    }

    loading = true

    try {
      await Users.register(data)
      const email = encodeURIComponent(data.email)
      goto(`/register/created?email=${email}`)
    } catch (error) {
      alert(error.response ? error.response.data : error.toString())
    }

    loading = false
  }
</script>

<div class="container-fluid mx-auto">
  <div class="row">
    <div class="col-xs-12 col-md-4">
      <form on:submit={submit}>
        <div class="form-group">
          <label for="email">
            E-mail address:
            <input
              class="form-control"
              type="email"
              id="email"
              bind:value={data.email}
              required />
          </label>
        </div>

        <div class="form-group">
          <label for="password">
            Password:
            <input
              class="form-control"
              id="password"
              type="password"
              bind:value={data.password}
              required />
          </label>
        </div>

        <div class="form-group">
          <label for="password">
            Repeat password:
            <input
              class="form-control"
              id="password"
              type="password"
              bind:this={passwordRepeat}
              required />
          </label>
        </div>

        <div class="form-group">
          <input
            type="submit"
            class="form-control btn btn-primary"
            value={loading ? 'Please wait...' : 'Register'}
            disabled={loading} />
        </div>
      </form>
    </div>
  </div>
</div>
