import { AxiosResponse, Method } from "axios";
import Controller from "./controller";


export default class Permissions extends Controller {
    public static async get(): Promise<AxiosResponse> {
        return this.client.get("/permissions/")
    }

    public static async remove(id: number): Promise<AxiosResponse> {
        return this.client.post(`/permissions/${id}/remove/`)
    }
}


