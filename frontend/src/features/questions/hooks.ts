import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { questionsApi, type CreateQuestionPayload } from '@/api/questions';

export function useQuestions() {
  return useQuery({
    queryKey: ['questions'],
    queryFn: () => questionsApi.list(),
  });
}

export function useQuestion(id: number) {
  return useQuery({
    queryKey: ['questions', id],
    queryFn: () => questionsApi.detail(id),
    enabled: Number.isFinite(id),
  });
}

export function useCreateQuestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateQuestionPayload) => questionsApi.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['questions'] });
    },
  });
}
