<script lang="ts">
  export let active: boolean = false
  export let dropdown: boolean = false
  export let name: string
  export let href: string
</script>

{#if dropdown}
  <a class="dropdown-item" {href}>{name}</a>
{:else}
  <li class="nav-item">
    <a class="nav-link" class:active {href}>
      {name}
      {#if active}<span class="sr-only">(current)</span>{/if}
    </a>
  </li>
{/if}
