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

                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#0" id="navbarDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Tasks
                    </a>
                    <div class="dropdown-menu" aria-labelledby="navbarDropdown">
                        <a class="dropdown-item" href="/request">Request data</a>
                        <a class="dropdown-item" href="/review">Review requests</a>
                    </div>
                </li>

                <NavItem name="Request data" href="/request" />
                <NavItem name="Review requests" href="/review" />
            {/if}

            <NavItem name="About" href="/about" active={segment === "about"} />
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
