<script lang="ts">
    import { goto } from '@sapper/app';
    import Users, {RegisterRequest} from "../../api/users";

    let data = new RegisterRequest("", "");
    let loading = false;

    async function handleClick() {
        loading = true;

        try {
            await Users.register(data);
            const email = encodeURIComponent(data.email);
            goto(`/register/created?email=${email}`);
        } catch (error) {
            alert(error.response ? error.response.data : error.toString());
        }

        loading = false;
    }
</script>

<div class="container">
    <div class="row">
        <div class="col-xs-12 col-md-4">
            <form>
                <div class="form-group">
                    <label for="email">
                        E-mail address:
                        <input
                            class="form-control"
                            type="email"
                            id="email"
                            bind:value={data.email}
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
                    <div
                        class="form-control btn btn-primary"
                        class:disabled={loading}
                        on:click={handleClick}
                    >
                        {loading ? "Please wait..." : "Register"}
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
