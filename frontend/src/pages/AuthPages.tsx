import { AuthCard, PublicLayout } from '../components/layout/PublicLayout'
import { EmailVerification } from '../features/auth/EmailVerification'
import { ForgotPasswordForm } from '../features/auth/ForgotPasswordForm'
import { LoginForm } from '../features/auth/LoginForm'
import { RegisterForm } from '../features/auth/RegisterForm'
import { ResetPasswordForm } from '../features/auth/ResetPasswordForm'

export function LoginPage() {
  return (
    <PublicLayout>
      <AuthCard title="Witaj ponownie" subtitle="Zaloguj się, aby zobaczyć swoje wydarzenia.">
        <LoginForm />
      </AuthCard>
    </PublicLayout>
  )
}

export function RegisterPage() {
  return (
    <PublicLayout>
      <AuthCard title="Załóż konto" subtitle="Zajmie to minutę. Konto przyda się przy kolejnych wydarzeniach KSM.">
        <RegisterForm />
      </AuthCard>
    </PublicLayout>
  )
}

export function VerifyEmailPage() {
  return (
    <PublicLayout>
      <AuthCard title="Potwierdzenie adresu e-mail">
        <EmailVerification />
      </AuthCard>
    </PublicLayout>
  )
}

export function ForgotPasswordPage() {
  return (
    <PublicLayout>
      <AuthCard title="Nie pamiętasz hasła?" subtitle="Podaj adres e-mail, a wyślemy link do ustawienia nowego hasła.">
        <ForgotPasswordForm />
      </AuthCard>
    </PublicLayout>
  )
}

export function ResetPasswordPage() {
  return (
    <PublicLayout>
      <AuthCard title="Ustaw nowe hasło">
        <ResetPasswordForm />
      </AuthCard>
    </PublicLayout>
  )
}
