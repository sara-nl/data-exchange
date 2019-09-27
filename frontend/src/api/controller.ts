import axios, {AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse} from "axios";

import { fromUrl } from "../stores/fromUrl";
import { token } from "../stores/token";

export default class Controller {
    public static client: AxiosInstance;
    public static sapper: any;

    public static setup(sapper: any) {
        this.sapper = sapper;
        this.client = axios.create({ baseURL: "/api/" });

        token.subscribe((value: string | null) => {
            this.token = value;
        });

        this.client.interceptors.request.use(
            (config: AxiosRequestConfig) => {
                if (this.token) {
                    config.headers.Authorization = `Token ${this.token}`;
                }

                return config;
            },
            async (error: AxiosError) => {
                throw error;
            },
        );

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
        );
    }

    protected static token: string | null;
}
