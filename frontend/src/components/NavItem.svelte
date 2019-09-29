<script lang="ts">
    export let active: boolean = false;
    export let name: string;
    export let href: string;
</script>

<li class="nav-item">
    <a class="nav-link" class:active={active} href={href}>
        {name}
        {#if active}<span class="sr-only">(current)</span>{/if}
    </a>
</li>
