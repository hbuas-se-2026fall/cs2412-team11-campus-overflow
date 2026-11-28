import { useEffect } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/shared/store/auth';

export function AppShell() {
  const me = useAuthStore((state) => state.me);
  const loading = useAuthStore((state) => state.loading);
  const bootstrap = useAuthStore((state) => state.bootstrap);
  const logout = useAuthStore((state) => state.logout);
  const navigate = useNavigate();

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  return (
    <div className="app">
      <header className="topbar">
        <Link className="brand" to="/questions">
          CampusOverflow
        </Link>
        <nav>
          <Link to="/questions">问题</Link>
          <Link to="/leaderboard">排行榜</Link>
          {me && <Link to="/questions/new">我要提问</Link>}
        </nav>
        <div className="spacer" />
        {loading ? (
          <span className="muted">加载中…</span>
        ) : me ? (
          <div className="user">
            <span>{me.displayName}</span>
            <span className="role">{me.role}</span>
            <button
              onClick={() => {
                void logout().then(() => navigate('/questions'));
              }}
            >
              退出
            </button>
          </div>
        ) : (
          <Link className="primary" to="/login">
            登录 / 注册
          </Link>
        )}
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
