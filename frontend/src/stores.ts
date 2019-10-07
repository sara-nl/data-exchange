import { writable } from "svelte/store";

function createWritableStore(key: string, startValue: any) {
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
        },
    };
};


export const fromUrl = writable(null);
export const token = createWritableStore("token", null);
export const email = createWritableStore("email", null);
