import { AxiosResponse } from "axios";
import Controller from "./controller";


export default class LoadFiles extends Controller {
    public static async start(): Promise<AxiosResponse> {
        return this.client.post("/runner/user_files/");
    }
}