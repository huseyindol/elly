# CI/CD Setup Guide — Elly CMS

Bu belge GitHub Actions CI/CD pipeline kurulumunu ve kullanımını açıklar.

## Mimari

```
Git Push (main) → GitHub Actions → Docker Build → GHCR Push → SSH Deploy → K3s
```

- **Build**: Maven test & package → Docker image → GHCR'a push (tag: commit SHA ilk 7 karakter)
- **Deploy**: SSH ile GCP VM'e bağlan → `kubectl set image` ile rolling update
- **Rollback**: GitHub Actions UI'dan eski tag'i gir → otomatik geri dön

---

## 1. Ön Gereksinimler

### Sunucu (GCP VM)
- Ubuntu 24.04 LTS
- k3s kurulu ve çalışıyor
- SSH key ile bağlantı

```bash
# İlk kurulum için:
chmod +x scripts/server-setup.sh
./scripts/server-setup.sh
```

### GitHub Repository
- Repo oluşturulmuş
- Private ise: GHCR erişimi için server'da pull secret gerekli

---

## 2. GitHub Secrets Tanımlama

**GitHub → Repo → Settings → Secrets and variables → Actions → New repository secret**

### Zorunlu Secrets

| Secret | Açıklama |
|---|---|
| `SERVER_HOST` | GCP VM IP adresi |
| `SERVER_USER` | SSH kullanıcısı |
| `SERVER_SSH_KEY` | SSH private key (tamamı) |

### Uygulama Secrets

| Secret | Açıklama |
|---|---|
| `TENANT1_DB_USER` | Tenant 1 DB kullanıcısı |
| `TENANT1_DB_PASSWORD` | Tenant 1 DB şifresi |
| `TENANT2_DB_USER` | Tenant 2 DB kullanıcısı |
| `TENANT2_DB_PASSWORD` | Tenant 2 DB şifresi |
| `JWT_SECRET` | JWT imzalama anahtarı |
| `JWT_ENCRYPTION_SECRET` | JWT şifreleme anahtarı |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |
| `FACEBOOK_CLIENT_ID` | Facebook App ID |
| `FACEBOOK_CLIENT_SECRET` | Facebook App Secret |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID |
| `GH_OAUTH_CLIENT_SECRET` | GitHub OAuth Client Secret |
| `EMAIL_API_KEY` | Email API key |

> **Not:** `GITHUB_TOKEN` otomatik sağlanır, tanımlamaya gerek yok. GitHub'da `GITHUB_CLIENT_SECRET` reserved olduğu için `GH_OAUTH_CLIENT_SECRET` kullanıyoruz.

---

## 3. GitHub Environment Oluşturma

**GitHub → Repo → Settings → Environments → New environment**

İsim: `production`

Bu environment deploy ve rollback workflow'larında koruma katmanı sağlar.
İsteğe bağlı olarak bu environment'a **production protection rules** ekleyebilirsin (required reviewers vs.).

---

## 4. Deployment Image Ayarı

`k8s/2a-app-deployment.yaml` içindeki `OWNER` kısmını kendi GitHub kullanıcı adınla değiştir:

```yaml
image: ghcr.io/OWNER/elly:latest
# ↓ Örnek:
image: ghcr.io/huseyindol/elly:latest
```

---

## 5. İlk Deploy

1. Tüm secret'ları GitHub'a ekle (Bölüm 2)
2. `production` environment oluştur (Bölüm 3)
3. Image path'i güncelle (Bölüm 4)
4. `main` branch'e push yap
5. **GitHub → Actions** sekmesinden deploy'u takip et

---

## 6. Rollback Kullanımı

Bir sorun çıkarsa önceki versiyona dönmek için:

1. **GitHub → Actions → "🔙 Rollback Deployment" → "Run workflow"**
2. `image_tag` alanına geri dönmek istediğin commit SHA'yı gir (ilk 7 karakter)
3. **Run workflow** butonuna tıkla

### Commit SHA'yı Bulma

```bash
# Son 10 commit'i listele
git log --oneline -10

# Örnek çıktı:
# abc1234 fix: login bug
# def5678 feat: new feature
# ghi9012 chore: update deps

# abc1234'e rollback yapmak için tag: abc1234
```

Veya: **GitHub → Packages → elly** sayfasından tüm versiyonları görebilirsin.

---

## 7. Dosya Yapısı

```
.github/
  workflows/
    deploy.yml          ← main'e push ile otomatik deploy
    rollback.yml        ← manuel rollback (workflow_dispatch)
k8s/
  0-namespace.yaml      ← Git'e gider
  1-configmap.yaml      ← Git'e gider (hassas olmayan config)
  1-secret.yaml         ← Git'e GİTMEZ (.gitignore)
  1-secret.template.yaml ← Git'e gider (placeholder'lı template)
  2a-app-deployment.yaml ← Git'e gider
  ...
```

---

## 8. Troubleshooting

### Deploy başarısız oldu
```bash
# Sunucuda pod loglarını kontrol et:
sudo kubectl logs -l app=elly-cms -n elly --tail=50

# Pod durumlarını gör:
sudo kubectl get pods -n elly

# Events'leri kontrol et:
sudo kubectl get events -n elly --sort-by='.lastTimestamp'
```

### Image pull hatası (Private repo)
```bash
# Pull secret oluştur:
sudo kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username=GITHUB_USERNAME \
  --docker-password=GITHUB_PAT \
  -n elly

# Deployment'a pull secret ekle:
# k8s/2a-app-deployment.yaml → spec.template.spec.imagePullSecrets
```

### Rollback çalışmıyor
```bash
# Mevcut image'ı kontrol et:
sudo kubectl get deployment elly-app -n elly \
  -o jsonpath='{.spec.template.spec.containers[0].image}'

# Manuel rollback:
sudo kubectl set image deployment/elly-app \
  elly-app=ghcr.io/OWNER/elly:TAG -n elly
```
