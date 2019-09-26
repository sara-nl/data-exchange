// store.js
import { writable } from "svelte/store";

const createWritableStore = (key: string, startValue: string | null) => {
    const { subscribe, set } = writable(startValue);

    return {
        subscribe,
        set(value: string | null) {
            set(value);
        },
        useLocalStorage() {
            const json = localStorage.getItem(key);
            if (json) {
                try {
                    set(JSON.parse(json));
                } catch (error) {
                    set(null);
                }
            }

            subscribe((current: string | null) => {
                localStorage.setItem(key, JSON.stringify(current));
            });
        }
    };
}

export const token = createWritableStore("token", null);
