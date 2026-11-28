import { useState } from 'react';
import { useLeaderboard } from '@/features/leaderboard/hooks';

const PERIODS = [
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'all', label: '全部' },
];

export function LeaderboardPage() {
  const [period, setPeriod] = useState('week');
  const { data, isLoading, error } = useLeaderboard(period);

  return (
    <div className="layout">
      <section>
        <div className="toolbar">
          <h1>排行榜</h1>
          <div className="spacer" />
          {PERIODS.map((item) => (
            <button
              key={item.value}
              className={period === item.value ? 'tag active' : 'tag'}
              onClick={() => setPeriod(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {isLoading && <p className="muted">加载中…</p>}
        {error && <p className="error">{error.message}</p>}

        {data && data.length === 0 && <p className="muted">暂无排名数据。</p>}

        <table className="board">
          <thead>
            <tr>
              <th>排名</th>
              <th>用户</th>
              <th>声誉</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((entry) => (
              <tr key={entry.userId}>
                <td>{entry.rank}</td>
                <td>
                  {entry.displayName}
                  {entry.verified && <span className="tag">已认证</span>}
                </td>
                <td>{entry.score}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
