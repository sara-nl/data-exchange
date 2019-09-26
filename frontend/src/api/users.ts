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

    constructor(token: string) {
        this.token = token;
    }
}

export class LoginRequest {
    public username: string = "";
    public password: string = "";

    constructor(username: string, password: string) {
        this.username = username;
        this.password = password;
    }
}

export default class Users extends Controller {
    public static async register(data: RegisterRequest): Promise<AxiosResponse> {
        return this.client.post("/users/register/", data);
    }

    public static async login(data: LoginRequest): Promise<AxiosResponse> {
        return this.client.post("/users/login/", data);
    }

    public static async activate(id: number, data: ActivateRequest): Promise<AxiosResponse> {
        return this.client.post(`/users/${id}/activate/`, data);
    }
}
