<script lang="ts">
  export let state: string = ''

  function class_(): string {
    switch (state) {
      case 'output_released':
        return 'text-success'

      case 'release_rejected':
        return 'text-danger'

      case 'running':
        return 'text-info'
      default:
        return ''
    }
  }

  function state_(): string {
    switch (state) {
      case 'running':
        return 'Running'

      case 'registered':
      case 'data_requested':
      case 'analyzing_algorithm':
        return 'Requested'

      case 'success':
      case 'error':
        return 'Reviewing'

      case 'output_released':
        return 'Passed'

      case 'release_rejected':
        return 'Rejected'

      default:
        return state
    }
  }
</script>

<span class={class_()}>{state_()}</span>
