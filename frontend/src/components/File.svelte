<script lang="typescript">
  export let name: string
  export let folder: boolean = false
</script>

<div class="file">
  <span class="fa-stack text-primary">
    <i class="fas fa-circle fa-stack-2x" />
    <i
      class="fas fa-stack-1x fa-inverse"
      class:fa-file={!folder}
      class:fa-folder={folder} />
  </span>
  {name}
</div>
