# OAuth2 Third-Party Authentication — KALDIRILDI

> **Durum: Bu özellik projeden tamamen kaldırıldı** (commit `7e160df`, Tem 2026).
> Google / Facebook / GitHub / X ile sosyal giriş artık desteklenmiyor.

## Neden kaldırıldı?

- Aktif kullanımı yoktu; bakım yükü ve güvenlik yüzeyi yaratıyordu.
- basedb'nin kaldırılmasıyla (tamamen tenant-scoped mimari) OAuth2 kullanıcılarının
  hangi tenant DB'sine yazılacağı belirsizleşti — mevcut akışla uyumsuzdu.

## Bugünkü kimlik doğrulama

| Akış | Endpoint | Açıklama |
|---|---|---|
| Panel login | `POST /api/v1/auth/login` (`loginType: "admin"`) | SUPER_ADMIN/ADMIN/EDITOR/VIEWER |
| Tenant login | `POST /api/v1/public/{tid}/auth/login` (`loginType: "tenant"`) | TENANT rolü |
| Register | `POST /api/v1/public/{tid}/auth/register` | TENANT rolü atar |
| MFA (TOTP) | `POST /api/v1/auth/mfa-verify` | Opsiyonel 2FA |

Detay: `docs/WEBSITE_AUTH_PAGES.md` ve `.claude/skills/spring-security-patterns/SKILL.md`.

## Geri getirmek gerekirse

Eski implementasyon (SecurityConfig OAuth2 client kayıtları, success handler, env değişkenleri)
git geçmişinde: `git show 7e160df^ -- src/main/java/com/cms/config/SecurityConfig.java`.
Geri eklerken tenant hedefi kararı (hangi tenant DB'sine kayıt) yeniden tasarlanmalıdır.
