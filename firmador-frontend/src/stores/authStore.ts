import { create } from 'zustand'

const ACCESS_TOKEN_KEY = 'access_token'
const DEV_GUEST_TOKEN = 'dev-guest-access'

type AuthState = {
  isAuthenticated: boolean
  isGuestMode: boolean
  setAuthenticated: (v: boolean) => void
  enableGuestMode: () => void
  logout: () => void
}

const currentToken = localStorage.getItem(ACCESS_TOKEN_KEY)

function isRealJwtToken(token: string | null) {
  return !!token && token !== DEV_GUEST_TOKEN
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: isRealJwtToken(currentToken),
  isGuestMode: currentToken === DEV_GUEST_TOKEN,
  setAuthenticated: (v: boolean) =>
    set(() => {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY)
      const isGuestMode = token === DEV_GUEST_TOKEN
      return {
        isAuthenticated: v && !isGuestMode,
        isGuestMode,
      }
    }),
  enableGuestMode: () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, DEV_GUEST_TOKEN)
    set(() => ({ isAuthenticated: true, isGuestMode: true }))
  },
  logout: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    set(() => ({ isAuthenticated: false, isGuestMode: false }))
  },
}))

export { ACCESS_TOKEN_KEY, DEV_GUEST_TOKEN }
export default useAuthStore
