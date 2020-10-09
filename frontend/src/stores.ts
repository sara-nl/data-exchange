import { onMount } from 'svelte'
import { readable, writable, Writable } from 'svelte/store'
import type { Share } from './api/shares'
import { getAllShares } from './api/shares'
import type { UserRole } from './api/users'

export type WritableStore<T> = Writable<T | null> & {
  useLocalStorage: () => void
}

function createWritableStore<T>(
  key: string,
  startValue: T | null
): WritableStore<T> {
  const { subscribe, set, update } = writable(startValue)

  // Check this: https://stackoverflow.com/a/61300826/401546
  // to better understand how this works.
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
          console.log('Error when writing into local storage', error)
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
export const mode = createWritableStore<UserRole>('mode', 'algorithm')

type EventualShares = Share[] | undefined
export const shares = readable<EventualShares>(
  undefined,
  (set: (value: EventualShares) => void) => {
    const updateShares: () => Promise<void> = () => {
      console.log('Updating shares')
      return getAllShares().then(set)
    }

    updateShares() // do it now
    const id = setInterval(updateShares, 5000)
    return () => clearInterval(id)
  }
)
