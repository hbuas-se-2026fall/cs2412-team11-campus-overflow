import { useQuery } from '@tanstack/react-query';
import { leaderboardApi } from '@/api/leaderboard';

export function useLeaderboard(period: string) {
  return useQuery({
    queryKey: ['leaderboard', period],
    queryFn: () => leaderboardApi.top(period),
  });
}
