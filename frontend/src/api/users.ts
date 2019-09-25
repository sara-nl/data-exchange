import { AxiosResponse } from "axios";
import Controller from "./controller";

export class RegisterRequest {
    public email: string = "";
    public password: string = "";

    constructor(email: string, password: string) {
        this.email = email;
        this.password = password;
    }
}

export class ActivateRequest {
    public token: string = "";
}

export default class Users extends Controller {
    public static async register(data: RegisterRequest): Promise<AxiosResponse> {
        return this.client.post("/users/register/", data);
    }

    public static async activate(data: ActivateRequest): Promise<AxiosResponse> {
        return this.client.post("/users/activate/", data);
    }
}
