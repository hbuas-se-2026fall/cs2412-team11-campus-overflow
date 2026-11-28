import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuestion } from '@/features/questions/hooks';
import { useAcceptAnswer, useCreateAnswer, useCreateComment, useVote } from '@/features/questions/useInteractions';
import { useAuthStore } from '@/shared/store/auth';
import type { Comment } from '@/api/types';

const EMPTY_ANSWER = '';
const EMPTY_COMMENT = '';

function CommentList({ comments }: { comments: Comment[] }) {
  if (comments.length === 0) return null;
  return (
    <ul className="commentlist">
      {comments.map((comment) => (
        <li key={comment.id}>
          <span className="muted">{comment.author.displayName}：</span>
          {comment.body}
        </li>
      ))}
    </ul>
  );
}

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const me = useAuthStore((state) => state.me);
  const { data, isLoading, error } = useQuestion(questionId);

  const vote = useVote(questionId);
  const createAnswer = useCreateAnswer(questionId);
  const acceptAnswer = useAcceptAnswer(questionId);
  const createComment = useCreateComment(questionId);

  const [answerDraft, setAnswerDraft] = useState(EMPTY_ANSWER);
  const [questionCommentDraft, setQuestionCommentDraft] = useState(EMPTY_COMMENT);
  const [answerCommentDrafts, setAnswerCommentDrafts] = useState<Record<number, string>>({});

  if (!Number.isFinite(questionId)) return <p className="error">问题编号无效</p>;
  if (isLoading) return <p className="muted">加载中…</p>;
  if (error) return <p className="error">{error.message}</p>;
  if (!data) return null;

  const mutationError = vote.error ?? createAnswer.error ?? acceptAnswer.error ?? createComment.error;

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
            <button
              disabled={!me || vote.isPending}
              onClick={() => vote.mutate({ targetType: 'QUESTION', targetId: data.id, value: 1 })}
              title="赞同"
            >
              ▲
            </button>
            <b>{data.score}</b>
            <button
              disabled={!me || vote.isPending}
              onClick={() => vote.mutate({ targetType: 'QUESTION', targetId: data.id, value: -1 })}
              title="反对"
            >
              ▼
            </button>
            <span className="muted">票</span>
          </div>
          <div className="body">
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

        <section className="comments">
          <h3>评论（{data.comments.length}）</h3>
          <CommentList comments={data.comments} />
          {me ? (
            <div className="commentform">
              <input
                value={questionCommentDraft}
                maxLength={600}
                placeholder="补充说明或提出澄清问题"
                onChange={(event) => setQuestionCommentDraft(event.target.value)}
              />
              <button
                disabled={createComment.isPending || questionCommentDraft.trim().length === 0}
                onClick={() => {
                  createComment.mutate(
                    { targetType: 'QUESTION', targetId: data.id, body: questionCommentDraft.trim() },
                    { onSuccess: () => setQuestionCommentDraft(EMPTY_COMMENT) },
                  );
                }}
              >
                发表评论
              </button>
            </div>
          ) : (
            <p className="muted">
              <Link to="/login">登录</Link> 后可评论
            </p>
          )}
        </section>

        <h2>{data.answerCount} 个回答</h2>
        {data.answers.length === 0 && <p className="muted">还没有回答。</p>}

        {data.answers.map((answer) => (
          <div className={`post ${data.acceptedAnswerId === answer.id ? 'is-accepted' : ''}`} key={answer.id}>
            <div className="votebox">
              <button
                disabled={!me || vote.isPending}
                onClick={() => vote.mutate({ targetType: 'ANSWER', targetId: answer.id, value: 1 })}
                title="赞同"
              >
                ▲
              </button>
              <b>{answer.score}</b>
              <button
                disabled={!me || vote.isPending}
                onClick={() => vote.mutate({ targetType: 'ANSWER', targetId: answer.id, value: -1 })}
                title="反对"
              >
                ▼
              </button>
              <span className="muted">票</span>
            </div>
            <div className="body">
              {data.acceptedAnswerId === answer.id && <p className="accepted">✓ 已采纳</p>}
              <div dangerouslySetInnerHTML={{ __html: answer.bodyHtml }} />
              <p className="muted">
                {answer.author.displayName} · {new Date(answer.createdAt).toLocaleString('zh-CN')}
              </p>

              <CommentList comments={answer.comments} />

              {me && data.permissions.canAccept && data.acceptedAnswerId !== answer.id && (
                <button disabled={acceptAnswer.isPending} onClick={() => acceptAnswer.mutate(answer.id)}>
                  采纳此回答
                </button>
              )}

              {me && (
                <div className="commentform">
                  <input
                    value={answerCommentDrafts[answer.id] ?? EMPTY_COMMENT}
                    maxLength={600}
                    placeholder="评论这个回答"
                    onChange={(event) =>
                      setAnswerCommentDrafts((prev) => ({ ...prev, [answer.id]: event.target.value }))
                    }
                  />
                  <button
                    disabled={
                      createComment.isPending || (answerCommentDrafts[answer.id] ?? EMPTY_COMMENT).trim().length === 0
                    }
                    onClick={() => {
                      createComment.mutate(
                        {
                          targetType: 'ANSWER',
                          targetId: answer.id,
                          body: (answerCommentDrafts[answer.id] ?? EMPTY_COMMENT).trim(),
                        },
                        {
                          onSuccess: () => setAnswerCommentDrafts((prev) => ({ ...prev, [answer.id]: EMPTY_COMMENT })),
                        },
                      );
                    }}
                  >
                    发表评论
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}

        {me ? (
          <section className="answerform">
            <h2>写下你的回答</h2>
            <textarea
              rows={8}
              value={answerDraft}
              onChange={(event) => setAnswerDraft(event.target.value)}
              placeholder="支持 Markdown 语法"
            />
            <button
              disabled={createAnswer.isPending || answerDraft.trim().length === 0}
              onClick={() => createAnswer.mutate(answerDraft.trim(), { onSuccess: () => setAnswerDraft(EMPTY_ANSWER) })}
            >
              发布回答
            </button>
          </section>
        ) : (
          <p className="muted">
            <Link to="/login">登录</Link> 后可回答
          </p>
        )}

        {mutationError && <p className="error">{mutationError.message}</p>}
      </article>
    </div>
  );
}
