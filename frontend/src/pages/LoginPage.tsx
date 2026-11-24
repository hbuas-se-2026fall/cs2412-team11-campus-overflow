import { useState, type ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/shared/store/auth';

type Mode = 'login' | 'register';

const EMPTY_FORM = {
  username: '',
  password: '',
  displayName: '',
  email: '',
  college: '',
  className: '',
};

export function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const [mode, setMode] = useState<Mode>('login');
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const setField = (key: keyof typeof EMPTY_FORM) => (event: ChangeEvent<HTMLInputElement>) => {
    setForm((previous) => ({ ...previous, [key]: event.target.value }));
  };

  const submit = async () => {
    setSubmitting(true);
    setError('');
    try {
      if (mode === 'register') {
        await authApi.register(form);
      }
      await login(form.username, form.password);
      navigate('/questions');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '操作失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="form-page narrow">
      <div className="tabs">
        <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
          登录
        </button>
        <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
          注册
        </button>
      </div>
      <label>
        学号 / 工号
        <input value={form.username} onChange={setField('username')} />
      </label>
      <label>
        密码
        <input type="password" value={form.password} onChange={setField('password')} />
      </label>
      {mode === 'register' && (
        <>
          <label>
            昵称
            <input value={form.displayName} onChange={setField('displayName')} />
          </label>
          <label>
            校园邮箱
            <input value={form.email} onChange={setField('email')} />
          </label>
          <label>
            学院
            <input value={form.college} onChange={setField('college')} />
          </label>
          <label>
            班级
            <input value={form.className} onChange={setField('className')} />
          </label>
        </>
      )}
      {error && <p className="error">{error}</p>}
      <button className="primary" disabled={submitting} onClick={submit}>
        {submitting ? '提交中…' : mode === 'login' ? '登录' : '注册并登录'}
      </button>
    </div>
  );
}
