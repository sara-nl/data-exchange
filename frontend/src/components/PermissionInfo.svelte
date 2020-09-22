<script lang="ts">
  import { permissionInfo, PermissionType } from '../api/permissions'

  export let permission: PermissionType
  export let user: string = 'algorithm'
</script>

<div class="PermissionInfo px-0 my-0 mx-0 py-0">
  <div class="container px-0 my-0 mx-0 py-0">
    {permissionInfo(permission, user)}
  </div>
</div>
