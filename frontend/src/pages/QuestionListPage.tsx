import { Link } from 'react-router-dom';
import { useQuestions } from '@/features/questions/hooks';
import { useAuthStore } from '@/shared/store/auth';

export function QuestionListPage() {
  const { data, isLoading, error } = useQuestions();
  const me = useAuthStore((state) => state.me);

  return (
    <div className="layout">
      <section>
        <div className="toolbar">
          <h1>全部问题</h1>
          <div className="spacer" />
          <Link className="primary" to="/questions/new">
            {me ? '我要提问' : '登录后提问'}
          </Link>
        </div>

        {isLoading && <p className="muted">加载中…</p>}
        {error && <p className="error">{error.message}</p>}

        <ul className="qlist">
          {data?.items.map((question) => (
            <li key={question.id}>
              <div className="stats">
                <span>
                  <b>{question.score}</b> 票
                </span>
                <span className={question.accepted ? 'accepted' : ''}>
                  <b>{question.answerCount}</b> 回答
                </span>
                <span>{question.viewCount} 浏览</span>
              </div>
              <div className="qmain">
                <Link className="qtitle" to={`/questions/${question.id}`}>
                  {question.title}
                </Link>
                <p className="excerpt">{question.excerpt}</p>
                <div className="tagrow">
                  {question.tags.map((tag) => (
                    <span key={tag} className="tag">
                      {tag}
                    </span>
                  ))}
                  <span className="muted">
                    {question.author.displayName} · {new Date(question.createdAt).toLocaleString('zh-CN')}
                  </span>
                </div>
              </div>
            </li>
          ))}
        </ul>

        {data && data.items.length === 0 && <p className="muted">还没有问题，点右上角发布第一个。</p>}
      </section>
    </div>
  );
}
