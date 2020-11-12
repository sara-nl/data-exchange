import Controller from './controller'
import type { Storage } from './storage'

export type Share = {
  id: number
  storage: Storage
  path: string
  isDirectory: boolean
  isAlgorithm: boolean
  webLink: string
}

// Depreacted
// use Share[]
export type SharesResponse = {
  own_algorithms: Share[]
  own_datasets: Share[]
}

// Depreacted
// use getAllShares()
export function getShares(): Promise<SharesResponse> {
  return Controller.client.get<Share[]>('/shares/').then((r) => {
    return {
      own_algorithms: r.data.filter((s) => s.isAlgorithm),
      own_datasets: r.data.filter((s) => !s.isAlgorithm),
    }
  })
}

export function getAllShares(): Promise<Share[]> {
  return Controller.client.get<Share[]>('/shares/').then((r) => r.data)
}
