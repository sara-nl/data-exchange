<script>
    import { token } from "../stores/token";
    import NavItem from "./NavItem";

    export let segment;
</script>

<nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a class="navbar-brand" href="/">Demo</a>

    <div class="navbar" id="navbarNav">
        <ul class="nav nav-pills mr-auto" style="border-right: 1px solid rgba(0, 0, 0, .12)">
            <NavItem name="Home" href="/" active={segment === undefined} />
            {#if $token}
                <NavItem name="My shared files" href="/myfiles" />
            {/if}

            <NavItem name="About" href="/about" active={segment === "about"} />
            <NavItem name="API" href="/api" />
            <NavItem name="Admin" href="/admin" />
        </ul>

        <ul class="nav ml-auto">
            {#if $token}
                <NavItem name="Log out" href="/logout" />
            {:else}
                <NavItem name="Sign in" href="/login" />
                <NavItem name="Register" href="/register" />
            {/if}
        </ul>
    </div>
</nav>
