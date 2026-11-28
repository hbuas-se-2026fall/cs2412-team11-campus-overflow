import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from '@/shared/layout/AppShell';
import { RequireAuth } from '@/shared/components/RequireAuth';
import { QuestionListPage } from '@/pages/QuestionListPage';
import { NewQuestionPage } from '@/pages/NewQuestionPage';
import { QuestionDetailPage } from '@/pages/QuestionDetailPage';
import { LoginPage } from '@/pages/LoginPage';
import { LeaderboardPage } from '@/pages/LeaderboardPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/questions" replace /> },
      { path: 'questions', element: <QuestionListPage /> },
      {
        path: 'questions/new',
        element: (
          <RequireAuth>
            <NewQuestionPage />
          </RequireAuth>
        ),
      },
      { path: 'questions/:id', element: <QuestionDetailPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'leaderboard', element: <LeaderboardPage /> },
    ],
  },
]);
