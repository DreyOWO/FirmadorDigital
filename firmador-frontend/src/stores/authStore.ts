import create from 'zustand'

type AuthState = {
  isAuthenticated: boolean
  setAuthenticated: (v: boolean) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: !!localStorage.getItem('access_token'),
  setAuthenticated: (v: boolean) => set(() => ({ isAuthenticated: v })),
}))

export default useAuthStore
