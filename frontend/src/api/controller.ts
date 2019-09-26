import axios, {AxiosInstance, AxiosRequestConfig, AxiosError, AxiosResponse} from "axios";

import { token } from "../stores/token";
import { fromUrl } from "../stores/fromUrl";

export default class Controller {
    public static client: AxiosInstance;
    public static sapper: any;
    protected static token: string | null;

    public static setup(sapper: any) {
        this.sapper = sapper;
        this.client = axios.create({ baseURL: "/api/" });

        token.subscribe((value: string | null) => {
            this.token = value;
        });

        this.client.interceptors.request.use(
            (config: AxiosRequestConfig) => {
                if (this.token) {
                    config.headers["Authorization"] = `Token ${this.token}`;
                }

                return config;
            },
            async (error: AxiosError) => {
                throw error;
            },
        )

        this.client.interceptors.response.use(
            (response: AxiosResponse) => response,
            async (error: AxiosError) => {
                if (error.response && error.response.status === 401) {
                    token.set(null);

                    const { page } = this.sapper.stores();
                    fromUrl.set(page.path);
                    sapper.goto("/login");
                } else {
                    throw error;
                }
            },
        )
    }
}
