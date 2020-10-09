<script lang="typescript">
  import { mode, shares } from '../../stores'
  import Spinner from '../Spinner.svelte'
</script>

{#if !Boolean($shares)}
  <Spinner />
{:else}
  <p class="h1 lead">
    You shared
    <span
      class="font-weight-bold {$mode === 'algorithm' ? 'text-primary' : 'text-secondary'}">{$shares.filter((s) => s.isAlgorithm).length}</span>
    algorithm(s) and
    <span
      class="font-weight-bold {$mode === 'data' ? 'text-primary' : 'text-secondary'}">{$shares.filter((s) => !s.isAlgorithm).length}</span>
    dataset(s)
  </p>
{/if}
