import { get } from '@/shared/lib/http';
import type { LeaderboardEntry } from '@/api/types';

export const leaderboardApi = {
  top: (period = 'week', limit = 20) => get<LeaderboardEntry[]>('/v1/leaderboard', { period, limit }),
};
