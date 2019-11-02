<script lang="ts">
    export let name: string;
    export let folder: boolean;
</script>

<style>
    .file {
        display: inline-block;
        margin: 5px 0;
    }
</style>

<div class="col-auto file">
    <span class="fa-stack fa-2x text-primary">
      <i class="fas fa-circle fa-stack-2x" />
      <i class="fas fa-stack-1x fa-inverse" class:fa-file={!folder} class:fa-folder={folder} />
    </span>
    {name}
</div>
