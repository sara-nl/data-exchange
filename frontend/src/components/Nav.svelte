<script>
    import { token, mode, email } from "../stores";
    import NavItem from "./NavItem";
    import NavDropdown from "./NavDropdown";

    export let segment;

    $: logout = $email ? `Log out (${$email})` : "Log out";
    $: modeText = $mode === "data" ? "Data owner" : "Algorithm owner";

    function toggleMode() {
        let current = $mode;
        mode.set(current === "data" ? "algorithm" : "data");
        console.log($mode);
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
    <a class="navbar-brand" href="/">DataExchange (Demo)</a>

    <div class="navbar justify-content-start" id="navbarNav">
        <ul class="nav nav-pills" style="border-right: 1px solid rgba(0, 0, 0, .12)">
            <NavItem name="Home" href="/" active={segment === undefined} />
            {#if $token}
                {#if $mode === "data"}
                    <NavItem name="Manage Data" href="/manage_data" />
                    <NavItem name="Review Requests" href="/requests" />
                {:else}
                    <NavItem name="Manage Algorithms" href="/manage_algorithms" />
                    <NavItem name="Create Requests" href="/tasks/request" />
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
                    class="btn btn-outline-primary"
                    type="button"
                    on:click={toggleMode}
                >
                    Toggle
                </button>
            </form>
        {/if}
    </div>
</nav>
