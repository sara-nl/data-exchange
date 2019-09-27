import Controller from "./controller";

export class RunnerLoadResponse {
    public output: string = "Loading";
}

export default class LoadRunner extends Controller {
    public static async start(): Promise<RunnerLoadResponse> {
        return this.client.post("/runner/shares_person/");
    }
}