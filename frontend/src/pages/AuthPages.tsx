import { AuthLayout } from '../components/layout/PublicLayout'
import { EmailVerification } from '../features/auth/EmailVerification'
import { ForgotPasswordForm } from '../features/auth/ForgotPasswordForm'
import { LoginForm } from '../features/auth/LoginForm'
import { RegisterForm } from '../features/auth/RegisterForm'
import { ResetPasswordForm } from '../features/auth/ResetPasswordForm'

export function LoginPage() {
  return (
    <AuthLayout title="Witaj ponownie" subtitle="Zaloguj się, aby zobaczyć swoje wydarzenia.">
        <LoginForm />
      </AuthLayout>
  )
}

export function RegisterPage() {
  return (
    <AuthLayout title="Załóż konto" subtitle="Zajmie to minutę. Konto przyda się przy kolejnych wydarzeniach KSM.">
        <RegisterForm />
      </AuthLayout>
  )
}

export function VerifyEmailPage() {
  return (
    <AuthLayout title="Potwierdzenie adresu e-mail">
        <EmailVerification />
      </AuthLayout>
  )
}

export function ForgotPasswordPage() {
  return (
    <AuthLayout title="Nie pamiętasz hasła?" subtitle="Podaj adres e-mail, a wyślemy link do ustawienia nowego hasła.">
        <ForgotPasswordForm />
      </AuthLayout>
  )
}

export function ResetPasswordPage() {
  return (
    <AuthLayout title="Ustaw nowe hasło">
        <ResetPasswordForm />
      </AuthLayout>
  )
}
