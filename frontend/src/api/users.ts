import axios, { AxiosResponse } from "axios";
import Controller from "./controller";

export type UserRole = "algorithm" | "data"

export class RegisterRequest {
    public email = "";
    public password = "";

    constructor(email: string, password: string) {
        this.email = email;
        this.password = password;
    }
}

export class ActivateRequest {
    public token = "";

    constructor(token: string) {
        this.token = token;
    }
}

export class LoginRequest {
    public username = "";
    public password = "";

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
        // Doesn't use this.client as we don't want the interceptors.
        return axios.post("/api/users/login/", data);
    }

    public static async activate(id: number, data: ActivateRequest): Promise<AxiosResponse> {
        return this.client.post(`/users/${id}/activate/`, data);
    }
}
