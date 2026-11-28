import { post, put } from '@/shared/lib/http';

export type TargetType = 'QUESTION' | 'ANSWER';

export interface VotePayload {
  targetType: TargetType;
  targetId: number;
  value: -1 | 0 | 1;
}

export interface CommentPayload {
  targetType: TargetType;
  targetId: number;
  body: string;
}

export const interactionsApi = {
  vote: (payload: VotePayload) => put<void>('/v1/votes', payload),
  createAnswer: (questionId: number, body: string) =>
    post<{ id: number }>(`/v1/questions/${questionId}/answers`, { body }),
  acceptAnswer: (questionId: number, answerId: number) =>
    put<void>(`/v1/questions/${questionId}/accepted-answer`, { answerId }),
  createComment: (payload: CommentPayload) => post<{ id: number }>('/v1/comments', payload),
};
