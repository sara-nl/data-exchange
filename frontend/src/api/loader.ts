import { AxiosResponse } from "axios";
import Controller from "./controller";



type LoadFilesResponse = {
    output: { 
        own_algorithms: {id: number, name: string},
        own_datasets: {id: number, name: string}
    }
}

export default function loadFiles(): Promise<AxiosResponse<LoadFilesResponse>> {
    return Controller.client.get("/loader/user_files/");
}