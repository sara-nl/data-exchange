import { AxiosResponse } from 'axios'
import Controller from './controller'
import { UserRole } from './users'
import { Share } from './shares'

export type PermissionsList = {
  list_permissions: string[][]
}

export type PermissionType =
  | 'one time permission'
  | 'stream permission'
  | 'One specific user permission'

export type Permission = {
  id: number
  algorithm: string
  algorithm_storage: Storage
  algorithm_etag?: string
  algorithm_report?: {
    chars: number
    lines: number
    words: number
    imports: string[]
    contents: { [fileName: string]: string }
  }
  algorithm_provider: string
  dataset: string
  dataset_storage: Storage
  dataset_provider: string
  state:
    | 'analyzing'
    | 'pending'
    | 'cancelled'
    | 'rejected'
    | 'active'
    | 'aborted'
  registered_on: Date
  updated_on: Date
  request_description: string
  permission_type: PermissionType
}

export type PermissionRequest = {
  algorithm: string
  algorithm_storage: string
  request_description: string
  permission_type: string
  dataset_provider: string
}

export async function requestPermission(
  ar: PermissionRequest
): Promise<Permission> {
  const response = await Controller.client.post<Permission>('/permissions/', ar)
  return response.data
}

export async function getPermission(id: number): Promise<Permission> {
  const response = await Controller.client.get<Permission>(`/permissions/${id}`)
  return response.data
}

export async function approvePermission(
  id: number,
  dataset: Share
): Promise<Permission> {
  const response = await Controller.client.put<Permission>(
    `/permissions/${id}/approve/`,
    { path: dataset.path, storage: dataset.storage }
  )
  return response.data
}

export async function rejectPermission(id: number): Promise<Permission> {
  const response = await Controller.client.put<Permission>(
    `/permissions/${id}/reject/`
  )
  return response.data
}

export async function headTaskId(id: number): Promise<{ id: number } | null> {
  const response = await Controller.client.get<{ id: number } | null>(
    `/permissions/${id}/head_task_id/`
  )
  return response.data
}

export async function getObtainerPerFile(): Promise<object> {
  const response = await Controller.client('/permissions/obtained_per_file/')
  return response.data
}

export type AllPermissions = {
  obtained_permissions: Permission[]
  given_permissions: Permission[]
}

export async function getAllPermissions(): Promise<AllPermissions> {
  const response = await Controller.client.get<AllPermissions>('/permissions/')
  return response.data
}

export default class Permissions extends Controller {
  public static async get(): Promise<AxiosResponse> {
    return this.client.get('/permissions/')
  }

  public static async getGivenPerFile(): Promise<AxiosResponse> {
    return this.client.get('/permissions/given_per_file/')
  }

  public static async remove(id: number): Promise<AxiosResponse> {
    return this.client.post(`/permissions/${id}/remove/`)
  }
}

export const permissionTypeLabels = {
  'one time permission': 'Run once',
  'stream permission': 'Run for a stream of datasets',
  'One specific user permission': 'Any algorithm on a dataset',
}

export const permissionApprovalActionDict = {
  'one time permission': 'Run algorithm on data to see output and go to step 2',
  'stream permission': 'Allow algorithm for stream of datasets',
  'One specific user permission':
    'Give Permission to run any algorithm on dataset',
}

export const permissionTypeDict = {
  algorithm: {
    'one time permission':
      'Selected algorithm will be ran on the selected dataset of the data owner exactly once.',
    'stream permission':
      'Every change in the selected data (or data folder) will automatically start a new run using this algorithm.',
    'One specific user permission':
      "You can always run all the algorithms you've shared to DataExchange on the selected dataset.",
  },
  data: {
    'one time permission':
      'Selected algorithm will be ran on your selected dataset once.',
    'stream permission':
      'Every change in the selected data (or data folder) will automatically start a new run using the given algorithm.',
    'One specific user permission':
      "The requesting user can run  all his algorithms on the dataset you're going to select at any time.",
  },
}

export function permissionInfo(
  permissionType: PermissionType,
  role: UserRole
): string {
  return permissionTypeDict[role][permissionType]!
}
