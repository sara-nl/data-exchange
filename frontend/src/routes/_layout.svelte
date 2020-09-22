<script>
  import Nav from '../components/Nav.svelte'

  export let segment
</script>

<style>
  main {
    position: relative;
    max-width: 90em;
    background-color: white;
    padding: 1em 2em;
    margin: 0 auto;
    box-sizing: border-box;
  }
</style>

<Nav {segment} />

<main>
  <slot />
</main>
