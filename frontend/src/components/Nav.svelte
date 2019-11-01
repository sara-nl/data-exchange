<script>
    import { token, email } from "../stores";
    import NavItem from "./NavItem";
    import NavDropdown from "./NavDropdown";

    export let segment;

    $: logout = $email ? `Log out (${$email})` : "Log out";
</script>

<nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a class="navbar-brand" href="/">Demo</a>

    <div class="navbar" id="navbarNav">
        <ul class="nav nav-pills mr-auto" style="border-right: 1px solid rgba(0, 0, 0, .12)">
            <NavItem name="Home" href="/" active={segment === undefined} />
            {#if $token}
                <NavItem name="Manage Data" href="/manage_data" />

                <NavItem name="My shared files" href="/myfiles" />

                <NavDropdown name="Tasks">
                    <NavItem dropdown name="Overview" href="/tasks" />
                    <NavItem dropdown name="Request permission" href="/tasks/request" />
                </NavDropdown>
                <NavDropdown name="Permissions">
                    <NavItem dropdown name="Permission overview" href="/permissions" />
                    <NavItem dropdown name="Permission logs" href="/permissions/logs" />
                    <NavItem dropdown name="Run with permission" href="/permissions/run" />

                </NavDropdown>
            {/if}

            <NavItem name="About" href="/about" active={segment === "about"} />
        </ul>

        <ul class="nav ml-auto">
            {#if $token}
                <NavItem name={logout} href="/logout" />
            {:else}
                <NavItem name="Sign in" href="/login" />
                <NavItem name="Register" href="/register" />
            {/if}
        </ul>
    </div>
</nav>
