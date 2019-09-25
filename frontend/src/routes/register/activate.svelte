<script>
    import { stores } from '@sapper/app';
    const { page } = stores();
    const { token } = $page.query;
</script>

{token}
