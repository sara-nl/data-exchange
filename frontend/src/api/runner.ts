import Controller from "./controller";

export class RunnerStartRequest {

    public algorithm_file: string = "";
    public data_file: string = "";
}

export class RunnerStartResponse {
    public output: string = "";
}

export default class Runner extends Controller {
    public static async start(data: RunnerStartRequest): Promise<RunnerStartResponse> {
        return this.client.post("/runner/start/", data);
    }
}
