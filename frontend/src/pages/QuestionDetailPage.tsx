import { Link, useParams } from 'react-router-dom';
import { useQuestion } from '@/features/questions/hooks';

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const { data, isLoading, error } = useQuestion(questionId);

  if (!Number.isFinite(questionId)) return <p className="error">问题编号无效</p>;
  if (isLoading) return <p className="muted">加载中…</p>;
  if (error) return <p className="error">{error.message}</p>;
  if (!data) return null;

  return (
    <div className="layout">
      <article>
        <p className="muted">
          <Link to="/questions">← 返回问题列表</Link>
        </p>
        <h1>{data.title}</h1>
        <p className="muted">
          {data.author.displayName} · {new Date(data.createdAt).toLocaleString('zh-CN')} · {data.viewCount} 次浏览
          {data.course && <> · 课程 {data.course.name}</>}
        </p>

        <div className="post">
          <div className="votebox">
            <b>{data.score}</b>
            <span className="muted">票</span>
          </div>
          <div className="body">
            {/* 正文由后端渲染并经 commonmark + OWASP Sanitizer 双重净化 */}
            <div dangerouslySetInnerHTML={{ __html: data.bodyHtml }} />
            <div className="tagrow">
              {data.tags.map((tag) => (
                <span key={tag} className="tag">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        </div>

        <h2>{data.answerCount} 个回答</h2>
        {data.answers.length === 0 && <p className="muted">还没有回答。</p>}
        {data.answers.map((answer) => (
          <div className={`post ${data.acceptedAnswerId === answer.id ? 'is-accepted' : ''}`} key={answer.id}>
            <div className="votebox">
              <b>{answer.score}</b>
              <span className="muted">票</span>
            </div>
            <div className="body">
              <div dangerouslySetInnerHTML={{ __html: answer.bodyHtml }} />
              <p className="muted">
                {answer.author.displayName} · {new Date(answer.createdAt).toLocaleString('zh-CN')}
              </p>
            </div>
          </div>
        ))}
      </article>
    </div>
  );
}
