import axios, {AxiosInstance} from "axios";

export default class Controller {
    public static client: AxiosInstance;

    public static setup() {
        this.client = axios.create({ baseURL: "/api/" });
    }
}
