import { useMutation, useQueryClient } from '@tanstack/react-query';
import { interactionsApi, type CommentPayload, type VotePayload } from '@/api/interactions';

function useInvalidateQuestion(questionId: number) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['questions', questionId] });
}

export function useVote(questionId: number) {
  const invalidate = useInvalidateQuestion(questionId);
  return useMutation({
    mutationFn: (payload: VotePayload) => interactionsApi.vote(payload),
    onSuccess: () => void invalidate(),
  });
}

export function useCreateAnswer(questionId: number) {
  const invalidate = useInvalidateQuestion(questionId);
  return useMutation({
    mutationFn: (body: string) => interactionsApi.createAnswer(questionId, body),
    onSuccess: () => void invalidate(),
  });
}

export function useAcceptAnswer(questionId: number) {
  const invalidate = useInvalidateQuestion(questionId);
  return useMutation({
    mutationFn: (answerId: number) => interactionsApi.acceptAnswer(questionId, answerId),
    onSuccess: () => void invalidate(),
  });
}

export function useCreateComment(questionId: number) {
  const invalidate = useInvalidateQuestion(questionId);
  return useMutation({
    mutationFn: (payload: CommentPayload) => interactionsApi.createComment(payload),
    onSuccess: () => void invalidate(),
  });
}
