<script>
    import { token, mode, email } from "../stores";
    import NavItem from "./NavItem";

    export let segment;

    $: logout = $email ? `Log out (${$email})` : "Log out";
    $: modeText = $mode === "data" ? "Data owner" : "Algorithm owner";

    function toggleMode() {
        let current = $mode;
        mode.set(current === "data" ? "algorithm" : "data");
    }
</script>

<style>
    .mode {
        margin-left: -5px;
    }

    .mode-inner {
        margin-left: 6px;
        margin-right: 12px;
    }
</style>

<nav class="navbar navbar-light bg-light">
    <a class="navbar-brand text-primary font-weight-bold" href="/">DataExchange (Demo)</a>

    <div class="navbar justify-content-left" id="navbarNav">
        <ul class="nav nav-pills" style="border-right: 1px solid rgba(0, 0, 0, .12)">
            <NavItem name="Home" href="/" active={segment === undefined} />
            {#if $token}
                {#if $mode === "data"}
                    <NavItem name="Manage Data" href="/manage_data" active={segment === "manage_data"} />
                    <NavItem name="Review Requests" href="/requests"  active={segment === "requests"}/>
                {:else}
                    <NavItem name="My permissions" href="/manage_algorithms"  active={segment === "manage_algorithms"}/>
                    <NavItem name="Create Request" href="/tasks/request"  active={segment === "tasks"}/>
                {/if}
            {/if}
        </ul>

        <ul class="nav">
            {#if $token}
                <NavItem name={logout} href="/logout" />
            {:else}
                <NavItem name="Sign in" href="/login" />
                <NavItem name="Register" href="/register" />
            {/if}
        </ul>

        {#if $token}
            <form class="form-inline">
                <span class="mode">
                    &mdash;
                    <span class="mode-inner">
                        {modeText}
                    </span>
                </span>
                <button
                    class="btn btn-light btn-sm"
                    type="button"
                    on:click={toggleMode}
                >
                    (Toggle)
                </button>
            </form>
        {/if}
    </div>
</nav>
