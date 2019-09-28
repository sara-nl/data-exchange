import { AxiosResponse } from "axios";
import Controller from "./controller";


export default class LoadRunner extends Controller {
    public static async start(): Promise<AxiosResponse> {
        return this.client.post("/runner/shares_person/");
    }
}