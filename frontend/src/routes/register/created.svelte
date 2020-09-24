<script>
  import { stores } from '@sapper/app'
  const { page } = stores()
  const { email } = $page.query
</script>

<div class="container-fluid mx-auto">
  <div class="row">
    <div class="col-xs-12 col-md-6">
      <h1>Account created</h1>

      <p>
        We just sent a confirmation email to
        <span class="text-secondary">{email}</span>. Please follow the link in
        this email to activate your account.
      </p>

      <p><a href="/login">Go to login page</a></p>
    </div>
  </div>
</div>
