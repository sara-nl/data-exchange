import { AxiosResponse, Method } from "axios";
import Controller from "./controller";

export class PermissionRequest {
    public data?: any;
    public per_file: boolean = false;
}

export default class Permissions extends Controller {
    public static async get(): Promise<AxiosResponse> {
        return this.client.get("/permissions/")
    }

    public static async get_obtained_per_file(): Promise<AxiosResponse> {
        return this.client.get("/permissions/obtained_per_file/")
    }

    public static async get_given_per_file(): Promise<AxiosResponse> {
        return this.client.get("/permissions/given_per_file/")
    }

    public static async remove(id: number): Promise<AxiosResponse> {
        return this.client.post(`/permissions/${id}/remove/`)
    }
}


