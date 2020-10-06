<script lang="typescript">
  export let id: string
  export let linkText: string
</script>

<p>
  <a
    class="font-weight-bold list-group-item list-group-item-action"
    data-toggle="collapse"
    href="#{id}"
    role="button"
    aria-expanded="false"
    aria-controls={id}>
    {linkText}
  </a>
</p>
<div {id} class="collapse">
  <slot />
</div>
