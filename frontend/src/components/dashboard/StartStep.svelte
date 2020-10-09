<script lang="typescript">
  export let title: string | undefined
</script>

<div class="card rounded mt-3">
  {#if Boolean(title)}
    <h5 class="card-title lead text-center mt-3">{title}</h5>
  {/if}
  <div class="card-body">
    <slot />
  </div>
</div>
