<script lang="ts">
  export let small: boolean = false
  export let loading: boolean = true
  export let text: string = 'Loading...'
  let completed: string = 'Completed:'
</script>

<!-- Slightly modified version of https://gist.github.com/Martaver/1573ecfd13e2ad2ba8de6f6ad98e581a -->
<style lang="scss">
  span {
    font-size: 0.8rem;
  }

  .loader {
    display: flex;
    flex-direction: column;
    align-items: center;

    &:not(.small) {
      width: 80px;
    }

    &.small {
      flex-direction: row;
    }
  }

  .circular {
    animation: rotate 2s linear infinite;
    height: 64px;
    width: 64px;

    .small & {
      margin: 2px 8px 2px 2px;

      height: 32px;
      width: 32px;
    }
  }

  .path {
    stroke: #333;
    stroke-dasharray: 1, 200;
    stroke-dashoffset: 0;
    animation: dash 1.5s ease-in-out infinite;
    stroke-linecap: round;

    .small & {
      stroke-width: 3px;
    }
  }

  @keyframes rotate {
    100% {
      transform: rotate(360deg);
    }
  }

  @keyframes dash {
    0% {
      stroke-dasharray: 1, 200;
      stroke-dashoffset: 0;
    }
    50% {
      stroke-dasharray: 89, 200;
      stroke-dashoffset: -35;
    }

    100% {
      stroke-dasharray: 89, 200;
      stroke-dashoffset: -124;
    }
  }
</style>

<div class="spinner">
  <div class="loader" class:small>
    {#if loading}
      <svg class="circular" viewBox="25 25 50 50">
        <circle
          class="path"
          cx="50"
          cy="50"
          r="20"
          fill="none"
          stroke-width="2"
          stroke-miterlimit="10" />
      </svg>
      <span class="ml-3">{text}</span>
    {:else}
      <span class="text-success mr-2 mt-2 mb-2">{completed}</span><span>
        {text}</span>
    {/if}
  </div>
</div>
