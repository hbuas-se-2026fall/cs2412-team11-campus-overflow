import { get, post } from '@/shared/lib/http';
import type { Page, QuestionDetail, QuestionSummary } from '@/api/types';

export interface CreateQuestionPayload {
  courseId?: number;
  title: string;
  body: string;
  tags: string[];
}

export const questionsApi = {
  list: (page = 1, size = 20) => get<Page<QuestionSummary>>('/v1/questions', { page, size }),
  detail: (id: number) => get<QuestionDetail>(`/v1/questions/${id}`),
  create: (payload: CreateQuestionPayload) => post<{ id: number }>('/v1/questions', payload),
};
