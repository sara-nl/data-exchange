import Controller from './controller'

export type Share = {
  id: number
  storage: Storage
  path: string
  isDirectory: boolean
  isAlgorithm: boolean
  webLink: string
}

export type SharesResponse = {
  own_algorithms: Share[]
  own_datasets: Share[]
}

export function getShares(): Promise<SharesResponse> {
  return Controller.client.get<Share[]>('/shares/').then((r) => {
    return {
      own_algorithms: r.data.filter((s) => s.isAlgorithm),
      own_datasets: r.data.filter((s) => !s.isAlgorithm),
    }
  })
}
