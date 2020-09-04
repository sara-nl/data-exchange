import type { AxiosResponse } from 'axios'
import Controller from './controller'
import type { Permission } from './permissions'

export interface Task {
  id: number
  state:
    | 'running'
    | 'success'
    | 'error'
    | 'release_rejected'
    | 'output_released'
  progress_state?: object
  author_email: string
  approver_email: string
  algorithm: string
  dataset: string
  output: string
  review_output: boolean
  permission: Permission
  registered_on: Date
  updated_on: Date
}

export async function spawnTaskFromPermission(
  permissionId: number
): Promise<Task> {
  const response = await Controller.client.post<Task>(
    `/tasks/${permissionId}/start_with_perm/`,
    {}
  )
  return response.data
}

export async function startWithUserPermisson(
  permissionId: number,
  algorithm: string
): Promise<Task> {
  return Controller.client
    .post<Task>(`/tasks/${permissionId}/start_with_user_perm/`, { algorithm })
    .then((r) => r.data)
}

export async function getTasksToReview(): Promise<Task[]> {
  return Controller.client
    .get<{ to_approve_requests: Task[] }>(`/tasks/`)
    .then((r) => r.data.to_approve_requests)
}

export default class Tasks extends Controller {
  public static async get(): Promise<AxiosResponse> {
    return this.client.get('/tasks/')
  }

  public static async getLogs(): Promise<AxiosResponse> {
    return this.client.get('/tasks/list_logs/')
  }

  public static async retrieve(id: number): Promise<Task> {
    return this.client.get(`/tasks/${id}/`).then((r) => r.data)
  }

  public static async release(
    id: number,
    data: { released: boolean }
  ): Promise<AxiosResponse> {
    return this.client.post(`/tasks/${id}/release/`, data)
  }
}
