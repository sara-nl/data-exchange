import { writable, Writable } from 'svelte/store'
import { UserRole } from './api/users'

export type WritableStore<T> = Writable<T | null> & {
  useLocalStorage: () => void
}

function createWritableStore<T>(
  key: string,
  startValue: T | null
): WritableStore<T> {
  const { subscribe, set, update } = writable(startValue)

  return {
    subscribe,
    set,
    update,
    useLocalStorage(): void {
      const json = localStorage.getItem(key)
      if (json) {
        try {
          set(JSON.parse(json))
        } catch (error) {
          set(null)
        }
      }

      subscribe((current: T | null) => {
        localStorage.setItem(key, JSON.stringify(current))
      })
    },
  }
}

export const fromUrl = writable(null)
export const token = createWritableStore<string>('token', null)
export const email = createWritableStore<string>('email', null)
export const mode = createWritableStore<UserRole>('mode', null)
