import { Link, useSearchParams } from 'react-router-dom';
import { useQuestions } from '@/features/questions/hooks';
import { usePopularTags, useQuestionsByTag } from '@/features/tags/hooks';
import { useAuthStore } from '@/shared/store/auth';

export function QuestionListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTag = searchParams.get('tag');
  const me = useAuthStore((state) => state.me);

  const all = useQuestions();
  const byTag = useQuestionsByTag(activeTag);
  const tags = usePopularTags();

  const data = activeTag ? byTag.data : all.data;
  const isLoading = activeTag ? byTag.isLoading : all.isLoading;
  const error = activeTag ? byTag.error : all.error;

  const selectTag = (tag: string | null) => {
    if (tag) {
      setSearchParams({ tag });
    } else {
      setSearchParams({});
    }
  };

  return (
    <div className="layout listlayout">
      <aside className="tagpanel">
        <h2>热门标签</h2>
        {tags.isLoading && <p className="muted">加载中…</p>}
        {tags.data?.length === 0 && <p className="muted">暂无标签</p>}
        <ul>
          {tags.data?.map((tag) => (
            <li key={tag.name}>
              <button
                className={activeTag === tag.name ? 'tag active' : 'tag'}
                onClick={() => selectTag(activeTag === tag.name ? null : tag.name)}
              >
                {tag.name} <span className="muted">{tag.questionCount}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <section>
        <div className="toolbar">
          <h1>{activeTag ? `标签：${activeTag}` : '全部问题'}</h1>
          {activeTag && (
            <button onClick={() => selectTag(null)} className="linklike">
              清除筛选
            </button>
          )}
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
                    <button key={tag} className="tag" onClick={() => selectTag(tag)}>
                      {tag}
                    </button>
                  ))}
                  <span className="muted">
                    {question.author.displayName} · {new Date(question.createdAt).toLocaleString('zh-CN')}
                  </span>
                </div>
              </div>
            </li>
          ))}
        </ul>

        {data && data.items.length === 0 && (
          <p className="muted">{activeTag ? `标签 ${activeTag} 下还没有问题。` : '还没有问题，点右上角发布第一个。'}</p>
        )}
      </section>
    </div>
  );
}