import { AxiosResponse, Method } from "axios";
import Controller from "./controller";


export default class Permissions extends Controller {
    public static async remove(name: string): Promise<AxiosResponse> {
        return this.client.delete(`/shares/${name}/remove/`)
    }
}