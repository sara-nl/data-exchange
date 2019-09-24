import { AxiosResponse } from "axios";
import Controller from "./controller";

export class RunnerStartRequest {
    public username: string = "";
    public password: string = "";

    public algorithm_file: string = "";
    public data_file: string = "";
}

export default class Runner extends Controller {
    public static async start(data: RunnerStartRequest): Promise<AxiosResponse> {
        return this.client.post("/runner/start/", data);
    }
}
