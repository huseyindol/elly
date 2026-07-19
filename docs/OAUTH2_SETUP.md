# Sosyal Giriş (Google / GitHub) — Kurulum ve Mimari

> Eski `spring-boot-starter-oauth2-client` tabanlı implementasyon `7e160df` ile kaldırılmıştı.
> Bu doküman Tem 2026'da eklenen **tenant-scoped yeniden tasarımı** anlatır.

## Kapsam ve Politika

- Sosyal giriş **yalnız tenant-site** akışıdır (kayıt + giriş, TENANT rolü).
- **Panelde sosyal giriş YOKTUR** (bilinçli karar) — panel şifre + MFA ile girer; sosyal
  kullanıcılar TENANT rolü aldığından panele zaten giremez (panel/tenant simetrisi).
- Aynı e-postayla şifreli hesap varsa **otomatik bağlanır** (provider e-postayı doğrulamıştır);
  panel rollü hesaplara bağlama **reddedilir**.
- **GitHub'da doğrulanmış e-posta zorunlu** — alınamazsa anlaşılır hata döner.

## Mimari (authorization-code, takas backend'de)

```
[Tenant Site] "Google ile devam et"
  → GET /api/auth/social/start/{provider}      (Next: state cookie + provider'a 302)
  → provider onay ekranı
  → GET /api/auth/social/callback/{provider}   (Next: state doğrula)
      → POST {API}/api/v1/public/{tid}/auth/social/{provider}  { code, redirectUri }
          [Backend] OAuthProviderClient: code → token takası (secret'lar burada),
                    profil doğrulama; AuthService.socialLogin: bul/bağla/oluştur
          → normal login ile AYNI DtoAuthResponse
      → httpOnly cookie'ler (writeAuthCookies) → redirect '/'
```

Backend sınıfları: `OAuthProviderClient` (takas + doğrulama), `AuthService.socialLogin`
(kullanıcı mantığı + `issueFullAuthToken` yeniden kullanımı), `AuthController /auth/social/{provider}`,
`PublicApiFilter` allowlist. Google id_token'ı TLS ile doğrudan alındığından imza doğrulaması
yapılmaz; `aud`/`iss`/`email_verified` kontrol edilir.

## Provider Konsol Kurulumu

### Google (console.cloud.google.com)
1. APIs & Services → Credentials → Create Credentials → **OAuth client ID** (Web application)
2. Authorized redirect URIs:
   - `http://localhost:3000/api/auth/social/callback/google` (dev)
   - `https://www.huseyindol.com/api/auth/social/callback/google` (prod — her tenant domain'i için ekle)
3. Client ID + Client Secret'ı al.

### GitHub (github.com → Settings → Developer settings → OAuth Apps)
1. **New OAuth App**; Authorization callback URL:
   - `http://localhost:3000/api/auth/social/callback/github` (dev için ayrı app önerilir)
   - `https://www.huseyindol.com/api/auth/social/callback/github` (prod)
2. Client ID + Client Secret'ı al.

## Env Değişkenleri

| Nerede | Değişken | Açıklama |
|---|---|---|
| Backend | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google takası |
| Backend | `GH_OAUTH_CLIENT_ID`, `GH_OAUTH_CLIENT_SECRET` | GitHub takası |
| Tenant (Vercel) | `NEXT_PUBLIC_GOOGLE_CLIENT_ID`, `NEXT_PUBLIC_GITHUB_CLIENT_ID` | Yalnız public ID'ler (authorize URL'i için) |

- Backend'de boş bırakılan provider **kapalı** kalır ("yapılandırılmamış" hatası).
- k8s: `1-secret.template.yaml` + `deploy.yml` envsubst hazır → GitHub Actions secrets'a
  4 değer eklenmeli. Canlı `elly-secret`'a elle eklemek için `kubectl patch` kullanılabilir.
- `NEXT_PUBLIC_*` build-time inline → Vercel'de env ekleyince **redeploy** şart.

## Davranış Özeti

| Senaryo | Sonuç |
|---|---|
| İlk kez Google/GitHub | Yeni TENANT kullanıcı (password NULL, emailVerified=true, doğrulama maili yok) |
| Aynı provider ile tekrar | Doğrudan giriş |
| E-posta şifreli hesapla eşleşti | Hesap bağlanır + giriş (şifreli giriş de çalışmaya devam eder) |
| E-posta panel rollü hesapla eşleşti | `403 — Panel hesapları sosyal girişle kullanılamaz` |
| GitHub e-postası gizli/doğrulanmamış | Anlaşılır hata, kayıt yok |
| MFA açık TENANT kullanıcısı | Normal login gibi `mfaToken` adımı |
