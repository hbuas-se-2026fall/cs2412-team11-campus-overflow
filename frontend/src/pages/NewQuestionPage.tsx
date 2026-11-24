import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateQuestion } from '@/features/questions/hooks';
import { useAuthStore } from '@/shared/store/auth';

export function NewQuestionPage() {
  const navigate = useNavigate();
  const me = useAuthStore((state) => state.me);
  const createQuestion = useCreateQuestion();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [tags, setTags] = useState('');
  const [courseId, setCourseId] = useState('');

  const submit = () => {
    createQuestion.mutate(
      {
        courseId: courseId ? Number(courseId) : undefined,
        title: title.trim(),
        body: body.trim(),
        tags: tags.split(/[,，\s]+/).filter(Boolean),
      },
      { onSuccess: ({ id }) => navigate(`/questions/${id}`) },
    );
  };

  return (
    <div className="form-page">
      <h1>提出一个问题</h1>
      <label>
        标题（10–150 字，尽量具体）
        <input value={title} onChange={(event) => setTitle(event.target.value)} />
      </label>
      <label>
        正文（Markdown，说明你尝试过什么、卡在哪里）
        <textarea rows={12} value={body} onChange={(event) => setBody(event.target.value)} />
      </label>
      <label>
        标签（1–5 个，用空格或逗号分隔）
        <input value={tags} onChange={(event) => setTags(event.target.value)} placeholder="数据结构 红黑树" />
      </label>
      <label>
        所属课程（可选）
        <select value={courseId} onChange={(event) => setCourseId(event.target.value)}>
          <option value="">不关联课程</option>
          {me?.courses.map((course) => (
            <option key={course.id} value={course.id}>
              {course.code} {course.name}
            </option>
          ))}
        </select>
      </label>

      {createQuestion.error && <p className="error">{createQuestion.error.message}</p>}
      <button className="primary" disabled={createQuestion.isPending} onClick={submit}>
        {createQuestion.isPending ? '发布中…' : '发布问题'}
      </button>
    </div>
  );
}
