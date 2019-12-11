import axios, {AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse} from "axios";
import { fromUrl, token } from "../stores";

export default class Controller {
    public static sapper: any;
    public static client: AxiosInstance = axios.create({ baseURL: "/api/" });

    public static setup(sapper: any) {
        this.sapper = sapper;

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
