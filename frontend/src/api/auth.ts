import { get, post } from '@/shared/lib/http';
import type { Me } from '@/api/types';

export interface RegisterPayload {
  username: string;
  password: string;
  displayName: string;
  email: string;
  college: string;
  className: string;
}

export const authApi = {
  csrf: () => get<{ token: string }>('/v1/auth/csrf'),
  me: () => get<Me>('/v1/auth/me'),
  login: (username: string, password: string) => post<Me>('/v1/auth/login', { username, password }),
  register: (payload: RegisterPayload) => post<{ id: number }>('/v1/auth/register', payload),
  logout: () => post<void>('/v1/auth/logout'),
};
