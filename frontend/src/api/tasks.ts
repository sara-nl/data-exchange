import { AxiosResponse, Method } from "axios";
import Controller from "./controller";

export class TasksStartRequest {
    public algorithm: string = "";
    public data_owner: string = "";
    public requested_data: string = "";
}

export class TasksReviewRequest {
    public data: Object = {};
    public approved: boolean = false;
}

export class TaskRetrieveRequest {
    public task_id: number = 0;
}


export default class Tasks extends Controller {
    public static async start(data: TasksStartRequest): Promise<AxiosResponse> {
        return this.client.post("/tasks/", data);
    }

    public static async get(): Promise<AxiosResponse> {
        return this.client.get("/tasks/")
    }

    public static async retrieve(id: number): Promise<AxiosResponse> {
        return this.client.get(`/tasks/${id}`)
    }

    public static async review(id: number, data: TasksReviewRequest): Promise<AxiosResponse> {
        return this.client.post(`/tasks/${id}/review/`, data)
    }

    public static async release(id: number, data: TaskRetrieveRequest): Promise<AxiosResponse> {
        return this.client.post(`/tasks/${id}/release/`, data)
    }
}
