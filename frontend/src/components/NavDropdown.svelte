<script>
    export let name;
</script>

<li class="nav-item dropdown">
    <a
        class="nav-link dropdown-toggle"
        href="#0"
        id="navbarDropdown"
        role="button"
        data-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
    >
        {name}
    </a>

    <div class="dropdown-menu" aria-labelledby="navbarDropdown">
        <slot />
    </div>
</li>
