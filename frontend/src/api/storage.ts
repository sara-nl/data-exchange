export type Storage = 'researchdrive' | 'googledrive'

export const StorageNames: { [k in Storage]: string } = {
  researchdrive: 'SURF Research Drive',
  googledrive: 'Google Drive',
}
