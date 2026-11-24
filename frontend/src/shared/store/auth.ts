import { create } from 'zustand';
import { authApi } from '@/api/auth';
import type { Me } from '@/api/types';

interface AuthState {
  me: Me | null;
  loading: boolean;
  bootstrap: () => Promise<void>;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  me: null,
  loading: true,

  bootstrap: async () => {
    try {
      await authApi.csrf();
    } catch {
      // CSRF Cookie 获取失败不影响读取当前登录态
    }
    try {
      set({ me: await authApi.me() });
    } catch {
      set({ me: null });
    } finally {
      set({ loading: false });
    }
  },

  login: async (username, password) => {
    set({ me: await authApi.login(username, password) });
  },

  logout: async () => {
    await authApi.logout();
    set({ me: null });
  },
}));
