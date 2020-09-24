<script>
  import { token, email } from '../stores'

  email.set(null)
  token.set(null)
</script>

<div class="container-fluid mx-auto">
  <div class="row">
    <div class="col-xs-12 col-md-6">
      <h1>Logged out successfully</h1>

      <p><a href="/login">Go to login page</a></p>
    </div>
  </div>
</div>
