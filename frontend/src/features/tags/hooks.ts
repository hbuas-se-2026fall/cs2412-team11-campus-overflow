import { useQuery } from '@tanstack/react-query';
import { searchApi, tagsApi } from '@/api/tags';

export function usePopularTags() {
  return useQuery({
    queryKey: ['tags', 'popular'],
    queryFn: () => tagsApi.popular(),
  });
}

export function useQuestionsByTag(tag: string | null) {
  return useQuery({
    queryKey: ['questions', 'tag', tag],
    queryFn: () => searchApi.byTag(tag ?? ''),
    enabled: Boolean(tag),
  });
}