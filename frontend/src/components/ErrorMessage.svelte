<script lang="ts">
  export let error: string
</script>

{#if error}
  <div class="alert alert-danger alert-dismissible show">
    <strong>Error:</strong>
    {error}
  </div>
{/if}
