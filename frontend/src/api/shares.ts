import { AxiosResponse, Method } from "axios";
import Controller from "./controller";


export default class RemoveShare extends Controller {
    public static async remove(file_id: string): Promise<AxiosResponse> {
        return this.client.delete(`/shares/${file_id}/remove/`)
    }
}