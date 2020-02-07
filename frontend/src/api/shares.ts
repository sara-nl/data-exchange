import { AxiosResponse } from "axios";
import Controller from "./controller";

export type Share = {id: number, name: string, isDirectory: boolean}

type SharesResponse = { 
    own_algorithms: Share[],
    own_datasets: Share[]
}

export function getShares(): Promise<SharesResponse> {
    return Controller.client.get<SharesResponse>("/shares/")
        .then(r => r.data);
}

export default class RemoveShare extends Controller {
    public static async remove(fileId: string): Promise<AxiosResponse> {
        return this.client.delete(`/shares/${fileId}/remove/`)
    }
}