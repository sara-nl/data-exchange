<script lang="ts">
    import { goto } from "@sapper/app";

    import Users, {LoginRequest} from "../api/users";
    import { token, email } from "../stores";

    let data = new LoginRequest("", "");
    let loading = false;

    async function submit(event: any) {
        event.preventDefault();

        loading = true;

        try {
            const { data: login } = await Users.login(data);
            token.set(login.token);
            email.set(data.username);
            goto("/");
        } catch (error) {
            const response = error.response;
            if (response) {
                const data = response.data;
                const detail = data.detail || data.non_field_errors[0];
                alert(detail);
            } else {
                throw error;
            }
        }

        loading = false;
    }
</script>

<div class="container">
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
                            bind:value={data.username}
                        >
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
                        >
                    </label>
                </div>

                <div class="form-group">
                    <input
                        type="submit"
                        class="form-control btn btn-primary"
                        value={loading ? "Please wait..." : "Sign in"}
                        disabled={loading}
                    >
                </div>
            </form>
        </div>
    </div>
</div>
