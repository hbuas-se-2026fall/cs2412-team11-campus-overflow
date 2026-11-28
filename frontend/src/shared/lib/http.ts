import axios, { type AxiosError } from 'axios';

/** 后端以 RFC 9457 Problem Details 返回错误，这里统一转成带业务码的异常。 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface ProblemBody {
  code?: string;
  detail?: string;
}

const MUTATING_METHODS = new Set(['post', 'put', 'patch', 'delete']);

function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  const entry = document.cookie.split('; ').find((item) => item.startsWith(prefix));
  if (!entry) return null;
  const raw = entry.slice(prefix.length);
  return raw.length > 0 ? decodeURIComponent(raw) : null;
}

export const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { Accept: 'application/json' },
});

// CSRF 双提交：写请求前从 XSRF-TOKEN Cookie 取 token 放进请求头，缺失时先请求一次 /auth/csrf
http.interceptors.request.use(async (config) => {
  const method = (config.method ?? 'get').toLowerCase();
  if (MUTATING_METHODS.has(method)) {
    let token = readCookie('XSRF-TOKEN');
    if (!token) {
      await axios.get('/api/v1/auth/csrf', { withCredentials: true });
      token = readCookie('XSRF-TOKEN');
    }
    if (token) config.headers.set('X-XSRF-TOKEN', token);
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemBody>) => {
    const status = error.response?.status ?? 0;
    const code = error.response?.data?.code ?? 'SYS-000';
    const detail = error.response?.data?.detail ?? error.message;
    return Promise.reject(new ApiError(status, code, detail));
  },
);

export function get<T>(url: string, params?: Record<string, string | number | boolean>): Promise<T> {
  return http.get<T>(url, { params }).then((response) => response.data);
}

export function post<T>(url: string, body?: unknown): Promise<T> {
  return http.post<T>(url, body).then((response) => response.data);
}

export function del<T>(url: string): Promise<T> {
  return http.delete<T>(url).then((response) => response.data);
}

export function put<T>(url: string, body?: unknown): Promise<T> {
  return http.put<T>(url, body).then((response) => response.data);
}
