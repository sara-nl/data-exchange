<script lang="typescript">
  import FaqItem from './FaqItem.svelte'
  import { email } from '../../stores'
  import shareRd from '../../../static/faq/share-rd.png'
  import shareGd1 from '../../../static/faq/share-gd-1.png'
  import shareGd2 from '../../../static/faq/share-gd-2.png'
  import shareIcon from '../../../static/faq/share.svg'
</script>

<h3 class="mb-3">Frequently asked questions:</h3>

<FaqItem
  id="howToShareResearchDrive"
  linkText="How to share files with SURF Research Drive?">
  <div class="card card-body text-muted">
    <p class="text-justify">
      ℹ&nbsp;&nbsp;&nbsp;Please note that your SURF Research Drive
      <i>username</i>
      must match the email in Data Exchange: currently
      <b>({$email})</b>.
    </p>
    <p>If it's not the same - create a new Data Exchange account.</p>
    <p>If your Research Drive username is not an email - use Google Drive.</p>
  </div>

  <div class="my-3">
    <p>
      <b>1.</b>
      In ResearchDrive: click on the share &nbsp;
      <img src={shareIcon} alt="Research Drive share icon" />
      &nbsp; icon next to the file or folder.
    </p>
    <p>
      <b>2.</b>
      Type in "data_exchange" as "users or groups to share with". Then select
      "Data Exchange" to share your file.
      <br />

      <img
        style="max-width: 300px"
        src={shareRd}
        alt="Share with Research Drive" />
    </p>
    <p>
      <b>3.</b>
      Eventually your files should become visible on Data Exchange. Due to
      internal caching, you may need to wait up to a minute and refresh the Data
      Exchange page.
    </p>
  </div>
</FaqItem>

<FaqItem
  id="howToShareGoogleDrive"
  linkText="How to share files with Google Drive?">
  <div class="card card-body text-muted">
    <p class="text-justify">
      ℹ&nbsp;&nbsp;&nbsp;Please note that your Google Drive email must match the
      email in Data Exchange: currently
      <b>({$email})</b>.
    </p>
    <p>
      If it's not the same - create a new Google Drive or Data Exchange account.
    </p>
  </div>

  <div class="my-3">
    <p>
      <b>1.</b>
      Locate the file or folder you want to share, right-click on it, choose
      "Share".
      <br />
      <img
        style="max-width: 300px"
        src={shareGd1}
        alt="Right-click and choose Share" />
    </p>
    <p>
      <b>2.</b>
      In the modal dialogue that opened, change the role to 'viewer', uncheck
      'Notify people' and enter the following account id:
      <br />
      <code
        class="small">data-exchange-drive@quickstart-1584612842886.iam.gserviceaccount.com</code>
      <br />
      <br />
      <img
        style="max-width: 500px"
        src={shareGd2}
        alt="Right-click and choose Share" />
    </p>
    <p><b>3.</b> Click "Share".</p>
    <p>
      <b>4.</b>
      Eventually your files should become visible on Data Exchange. Due to
      internal caching, you may need to wait up to a minute and refresh the Data
      Exchange page.
    </p>
  </div>
</FaqItem>
