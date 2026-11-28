import { get } from '@/shared/lib/http';
import type { Page, SearchResult } from '@/api/types';

export interface TagView {
  name: string;
  questionCount: number;
}

export const tagsApi = {
  popular: (limit = 20) => get<TagView[]>('/v1/tags/popular', { limit }),
};

export const searchApi = {
  byTag: (tag: string) => get<Page<SearchResult>>('/v1/search/questions', { tag }),
};