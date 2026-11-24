import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/shared/store/auth';

export function RequireAuth({ children }: { children: ReactNode }) {
  const me = useAuthStore((state) => state.me);
  const loading = useAuthStore((state) => state.loading);

  if (loading) return <p className="muted">加载中…</p>;
  return me ? <>{children}</> : <Navigate to="/login" replace />;
}
