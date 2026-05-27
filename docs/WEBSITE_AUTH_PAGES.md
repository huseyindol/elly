# Website — Kayıt & Giriş Sayfaları

**Hedef:** Website Next.js projesine kullanıcı kaydı ve tenant login akışı ekle.
E-posta doğrulama zorunludur; doğrulanmamış kullanıcılar giriş yapamaz.

**Stack:** Next.js 16 App Router, React 19, TypeScript 5.9, Tailwind CSS 4, shadcn/ui, Bun.
API çağrıları `fetcher` utility ile yapılır (token otomatik eklenir).
Tenant kimliği: her sayfada `tenantId` sabit olarak kodlanır veya `.env.local`'dan alınır.

---

## API Referansı

```
POST /api/v1/auth/register
Body: { username, email, password, firstName?, lastName?, tenantId }
Response: { result: true, data: { userId, username, email, message: "Kayıt başarılı. Lütfen e-postanızı doğrulayın." } }
→ JWT dönmez; kullanıcı e-postayı doğrulayana kadar giriş yapamaz.

POST /api/v1/auth/login
Body: { usernameOrEmail, password, tenantId, loginType: "tenant" }
Response: { result: true, data: { token, refreshToken, userId, username, email, roles, permissions } }
Cookie: accessToken, refreshToken, userCode, expiredDate (backend set eder)
→ E-posta doğrulanmamışsa 400 döner: "E-posta adresiniz henüz doğrulanmamış."

GET /api/v1/auth/verify?token={token}&tenantId={tenantId}
Response: { result: true, data: true }
→ Başarılı: kullanıcı aktifleşir, login sayfasına yönlendir.
```

**Response wrapper:** `{ result: boolean, message?: string, data: T }`

**Hata formatı:**
```json
{ "result": false, "message": "..." }
```

---

## Ortam Değişkeni

`.env.local`'a ekle:
```
NEXT_PUBLIC_TENANT_ID=tenant1
```

---

## 1. Servis Fonksiyonları

`src/app/_services/auth.services.ts` dosyasına ekle (yoksa oluştur):

```typescript
import { fetcher } from '@/utils/services/fetcher'

interface BaseResponse<T> {
  result: boolean
  message?: string
  data: T
}

interface RegisterPayload {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
  tenantId: string
}

interface RegisterResult {
  userId: number
  username: string
  email: string
  message: string
}

interface LoginPayload {
  usernameOrEmail: string
  password: string
  tenantId: string
  loginType: 'tenant'
}

interface LoginResult {
  token: string
  refreshToken: string
  userId: number
  username: string
  email: string
  roles: string[]
  permissions: string[]
}

export const registerService = async (payload: RegisterPayload): Promise<RegisterResult> => {
  const res = await fetcher<BaseResponse<RegisterResult>>('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.result) throw new Error(res.message ?? 'Kayıt başarısız')
  return res.data
}

export const loginService = async (payload: LoginPayload): Promise<LoginResult> => {
  const res = await fetcher<BaseResponse<LoginResult>>('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.result) throw new Error(res.message ?? 'Giriş başarısız')
  return res.data
}

export const verifyEmailService = async (token: string, tenantId: string): Promise<void> => {
  const params = new URLSearchParams({ token, tenantId })
  const res = await fetcher<BaseResponse<boolean>>(`/api/v1/auth/verify?${params}`, {
    method: 'GET',
  })
  if (!res.result) throw new Error(res.message ?? 'Doğrulama başarısız')
}
```

---

## 2. Kayıt Sayfası

`src/app/(website)/register/page.tsx` (veya projenin route yapısına uygun path):

```typescript
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { registerService } from '@/app/_services/auth.services'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { CheckCircle, AlertCircle } from 'lucide-react'

const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID ?? 'tenant1'

export default function RegisterPage() {
  const router = useRouter()
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    passwordConfirm: '',
    firstName: '',
    lastName: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (form.password !== form.passwordConfirm) {
      setError('Şifreler eşleşmiyor.')
      return
    }
    if (form.password.length < 6) {
      setError('Şifre en az 6 karakter olmalıdır.')
      return
    }

    setLoading(true)
    try {
      await registerService({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim() || undefined,
        lastName: form.lastName.trim() || undefined,
        tenantId: TENANT_ID,
      })
      setSuccess(true)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Bir hata oluştu.')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center p-4">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6">
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
              <div>
                <h2 className="text-xl font-semibold">Neredeyse tamam!</h2>
                <p className="mt-2 text-sm text-muted-foreground">
                  <strong>{form.email}</strong> adresine bir doğrulama e-postası gönderdik.
                  Lütfen e-postanızdaki bağlantıya tıklayarak hesabınızı etkinleştirin.
                </p>
              </div>
              <Button variant="outline" onClick={() => router.push('/login')}>
                Giriş Sayfasına Git
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Hesap Oluştur</CardTitle>
          <CardDescription>Ücretsiz hesap oluşturun ve hemen başlayın.</CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="firstName">Ad</Label>
                <Input
                  id="firstName"
                  name="firstName"
                  value={form.firstName}
                  onChange={handleChange}
                  placeholder="Adınız"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="lastName">Soyad</Label>
                <Input
                  id="lastName"
                  name="lastName"
                  value={form.lastName}
                  onChange={handleChange}
                  placeholder="Soyadınız"
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label htmlFor="username">Kullanıcı Adı *</Label>
              <Input
                id="username"
                name="username"
                required
                value={form.username}
                onChange={handleChange}
                placeholder="kullaniciadi"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="email">E-posta *</Label>
              <Input
                id="email"
                name="email"
                type="email"
                required
                value={form.email}
                onChange={handleChange}
                placeholder="ornek@mail.com"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="password">Şifre *</Label>
              <Input
                id="password"
                name="password"
                type="password"
                required
                value={form.password}
                onChange={handleChange}
                placeholder="En az 6 karakter"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="passwordConfirm">Şifre Tekrar *</Label>
              <Input
                id="passwordConfirm"
                name="passwordConfirm"
                type="password"
                required
                value={form.passwordConfirm}
                onChange={handleChange}
                placeholder="Şifrenizi tekrar girin"
              />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Kaydediliyor...' : 'Hesap Oluştur'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Zaten hesabınız var mı?{' '}
            <a href="/login" className="text-primary hover:underline font-medium">
              Giriş Yap
            </a>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
```

---

## 3. E-posta Doğrulama Sayfası

`src/app/(website)/verify-email/page.tsx`:

```typescript
'use client'

import { useEffect, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { verifyEmailService } from '@/app/_services/auth.services'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { CheckCircle, AlertCircle, Loader2 } from 'lucide-react'

export default function VerifyEmailPage() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const token = searchParams.get('token') ?? ''
  const tenantId = searchParams.get('tenantId') ?? process.env.NEXT_PUBLIC_TENANT_ID ?? 'tenant1'

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setErrorMessage('Doğrulama bağlantısı geçersiz.')
      return
    }

    verifyEmailService(token, tenantId)
      .then(() => setStatus('success'))
      .catch((e: unknown) => {
        setStatus('error')
        setErrorMessage(e instanceof Error ? e.message : 'Doğrulama başarısız.')
      })
  }, [token, tenantId])

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardContent className="pt-6">
          {status === 'loading' && (
            <div className="flex flex-col items-center gap-4 text-center">
              <Loader2 className="h-10 w-10 animate-spin text-primary" />
              <p className="text-muted-foreground">E-posta doğrulanıyor...</p>
            </div>
          )}

          {status === 'success' && (
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
              <div>
                <h2 className="text-xl font-semibold">E-posta Doğrulandı!</h2>
                <p className="mt-2 text-sm text-muted-foreground">
                  Hesabınız başarıyla etkinleştirildi. Artık giriş yapabilirsiniz.
                </p>
              </div>
              <Button onClick={() => router.push('/login')}>Giriş Yap</Button>
            </div>
          )}

          {status === 'error' && (
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle className="h-8 w-8 text-destructive" />
              </div>
              <div>
                <h2 className="text-xl font-semibold">Doğrulama Başarısız</h2>
                <p className="mt-2 text-sm text-muted-foreground">{errorMessage}</p>
              </div>
              <Button variant="outline" onClick={() => router.push('/register')}>
                Tekrar Kayıt Ol
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
```

---

## 4. Giriş Sayfası

`src/app/(website)/login/page.tsx`:

```typescript
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { loginService } from '@/app/_services/auth.services'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { AlertCircle } from 'lucide-react'

const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID ?? 'tenant1'

export default function LoginPage() {
  const router = useRouter()
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      await loginService({
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
        tenantId: TENANT_ID,
        loginType: 'tenant',
      })
      // Cookie'ler backend tarafından set edildi
      router.push('/')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Giriş başarısız.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Giriş Yap</CardTitle>
          <CardDescription>Hesabınıza erişin.</CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1">
              <Label htmlFor="usernameOrEmail">Kullanıcı Adı veya E-posta</Label>
              <Input
                id="usernameOrEmail"
                name="usernameOrEmail"
                required
                value={form.usernameOrEmail}
                onChange={handleChange}
                placeholder="kullaniciadi veya ornek@mail.com"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="password">Şifre</Label>
              <Input
                id="password"
                name="password"
                type="password"
                required
                value={form.password}
                onChange={handleChange}
                placeholder="Şifreniz"
              />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Hesabınız yok mu?{' '}
            <a href="/register" className="text-primary hover:underline font-medium">
              Kayıt Ol
            </a>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
```

---

## 5. fetcher ve Login Davranışı Notu

`loginService` backend'e POST atar. Backend cookie'leri set eder
(`accessToken`, `refreshToken`, `userCode`, `expiredDate`).
`fetcher` bu cookie'leri otomatik okur — response'ta token yoksa da `router.push('/')` ile
yönlendirme yapılabilir.

Backend 400 döndüğünde `fetcher` hata fırlatır; `catch` bloğu error mesajını set eder.
E-posta doğrulanmamış kullanıcı için mesaj: **"E-posta adresiniz henüz doğrulanmamış. Lütfen e-postanızı kontrol edin."**

---

## 6. Doğrulama Kriterleri

1. `/register` → form doldur → POST → başarı ekranı ("E-postanızı kontrol edin")
2. Gelen e-postadaki linke tıkla → `/verify-email?token=...&tenantId=...` → yeşil başarı ekranı
3. `/login` → doğrulanmamış kullanıcı → hata: "E-posta adresiniz henüz doğrulanmamış"
4. `/login` → doğrulanmış kullanıcı → cookie set → ana sayfaya yönlendir
5. `bun dev` veya `bun run build` TypeScript hatası yok
