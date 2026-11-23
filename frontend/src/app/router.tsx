import { createBrowserRouter } from 'react-router'
import { AppShell } from '../components/layout/AppShell'
import { RedirectIfAuthenticated, RequireAdmin, RequireAuth } from '../components/layout/RouteGuards'
import { AccountPage } from '../pages/AccountPage'
import { AssignmentPage } from '../pages/AssignmentPage'
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage, VerifyEmailPage } from '../pages/AuthPages'
import { DashboardPage } from '../pages/DashboardPage'
import { EventPage } from '../pages/EventPage'
import { JoinPage } from '../pages/JoinPage'
import { LandingPage } from '../pages/LandingPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { PrivacyPage } from '../pages/PrivacyPage'
import { JoinRoute } from '../pages/PublicJoinPage'

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/privacy', element: <PrivacyPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    element: <RedirectIfAuthenticated />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
    ],
  },
  { path: '/join', element: <JoinRoute />, children: [{ index: true, element: <JoinPage /> }] },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/events/:eventId', element: <EventPage /> },
          { path: '/events/:eventId/assignment', element: <AssignmentPage /> },
          { path: '/account', element: <AccountPage /> },
          {
            element: <RequireAdmin />,
            children: [
              { path: '/admin', lazy: async () => ({ Component: (await import('../pages/AdminPages')).AdminEventsPage }) },
              { path: '/admin/events/new', lazy: async () => ({ Component: (await import('../pages/AdminPages')).AdminCreateEventPage }) },
              { path: '/admin/events/:eventId', lazy: async () => ({ Component: (await import('../pages/AdminPages')).AdminEventPage }) },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
