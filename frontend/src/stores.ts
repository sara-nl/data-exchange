import { writable, Writable } from "svelte/store";

function createPersistent<T>(key: string, startValue: T | null): Writable<T | null> {
    const svelteStore = writable(startValue);
    const json = localStorage.getItem(key);
    if (json) {
        try {
            svelteStore.set(JSON.parse(json))
        } catch (error) {
            console.error(`There was an error parsing value for key ${key}`)
            svelteStore.set(null)
        }
    }

    svelteStore.subscribe(current => localStorage.setItem(key, JSON.stringify(current)));
    return svelteStore;
}


export const fromUrl = writable(null);
export const token = createPersistent("token", null);
export const email = createPersistent("email", null);
export const mode = createPersistent("mode", null);
