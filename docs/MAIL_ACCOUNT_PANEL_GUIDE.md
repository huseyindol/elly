# Elly Admin Panel — Mail Hesabı Yönetimi Geliştirme Bağlamı

## Bu Dosya Ne İçin?

Bu dosya `elly-admin-panel` frontend reposunda mail hesabı yönetimi modülünü
sıfırdan geliştirmek için gereken tüm bağlamı içerir. Backend (`huseyindol/elly`)
tarafındaki implementasyon tamamlanmıştır; bu repo yalnızca UI katmanıdır.

---

## Backend Özeti (Tamamlandı)

**Branch:** `claude/fix-cors-error-cytcY`  
**Base URL:** `https://api.domain.com/api/v1`

### Neden Bu Geliştirme Yapıldı?

Önceki tasarımda SMTP credentials K8s manifest'te env var olarak tutuluyordu.
Artık her tenant kendi DB'sinde `mail_accounts` tablosunda birden fazla mail
hesabı (örn. `info@`, `sales@`, `noreply@`) saklayabilir. Şifreler AES-256-CBC
ile şifreli tutulur; API response'larda **asla** dönmez.

---

## API Endpoint Referansı

### 1. Tüm Hesapları Listele
```
GET /api/v1/mail-accounts
Authorization: Bearer <token>

Response 200:
{
  "result": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "Satış Hesabı",
      "fromAddress": "sales@firma.com",
      "smtpHost": "smtp.gmail.com",
      "smtpPort": 587,
      "smtpUsername": "sales@firma.com",
      "isDefault": true,
      "active": true,
      "createdAt": "2026-04-04T10:00:00.000+00:00",
      "updatedAt": "2026-04-04T10:00:00.000+00:00"
    }
  ]
}
```

### 2. Hesap Detayı
```
GET /api/v1/mail-accounts/{id}
Authorization: Bearer <token>

Response 200: { "result": true, "data": { ...DtoMailAccountResponse } }
```

### 3. Yeni Hesap Ekle
```
POST /api/v1/mail-accounts
Authorization: Bearer <token>
Content-Type: application/json

Body:
{
  "name": "Satış Hesabı",
  "fromAddress": "sales@firma.com",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "sales@firma.com",
  "smtpPassword": "gmail-app-password-16-char",
  "isDefault": false,
  "active": true
}

Validasyonlar:
- name: zorunlu, boş olamaz
- fromAddress: zorunlu, geçerli email formatı
- smtpHost: zorunlu
- smtpPort: zorunlu, 1-65535 aralığı
- smtpUsername: zorunlu
- smtpPassword: OLUŞTURURKEN zorunlu

Response 201: { "result": true, "data": { ...DtoMailAccountResponse } }
```

### 4. Hesap Güncelle
```
PUT /api/v1/mail-accounts/{id}
Authorization: Bearer <token>
Content-Type: application/json

Body: (POST ile aynı yapı)
Not: smtpPassword boş bırakılırsa mevcut şifre korunur.

Response 200: { "result": true, "data": { ...DtoMailAccountResponse } }
```

### 5. Hesap Sil
```
DELETE /api/v1/mail-accounts/{id}
Authorization: Bearer <token>

Response 200: { "result": true, "data": true }
```

### 6. Varsayılan Yap
```
PUT /api/v1/mail-accounts/{id}/default
Authorization: Bearer <token>

Response 200: { "result": true, "data": { ...DtoMailAccountResponse (isDefault: true) } }
Hata: Pasif hesap varsayılan yapılamaz → 400 Bad Request
```

### 7. SMTP Bağlantı Testi (Gerçek Mail Gönderir)
```
POST /api/v1/mail-accounts/{id}/test
Authorization: Bearer <token>
Content-Type: application/json

Body:
{
  "testTo": "admin@firma.com"
}

Response 200: { "result": true, "data": "Test maili başarıyla gönderildi → admin@firma.com" }
Hata 400: { "result": false, "message": "SMTP bağlantısı başarısız [smtp.gmail.com:587]: ..." }
```

### 8. Form Tanımı Oluştur/Güncelle (Mevcut endpoint güncellendi)
```
POST /api/v1/forms
PUT  /api/v1/forms/{id}
Authorization: Bearer <token>

Body'e eklenen yeni alan:
{
  ...(mevcut form alanları),
  "mailAccountId": 2   // null olursa varsayılan hesap kullanılır
}
```

### 9. Email Gönder (Mevcut endpoint güncellendi)
```
POST /api/v1/emails/send
Authorization: Bearer <token>

Body'e eklenen yeni alan:
{
  "to": "...",
  "subject": "...",
  "templateName": "...",
  "dynamicData": {},
  "mailAccountId": 2   // null olursa varsayılan hesap kullanılır
}
```

---

## DTO Yapıları

### DtoMailAccountResponse (API'den gelen)
```typescript
interface MailAccount {
  id: number
  name: string
  fromAddress: string
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  isDefault: boolean
  active: boolean
  createdAt: string
  updatedAt: string
  // smtpPassword: ASLA DÖNMEZ
}
```

### DtoMailAccountRequest (API'ye gönderilen)
```typescript
interface MailAccountRequest {
  name: string              // zorunlu
  fromAddress: string       // zorunlu, email format
  smtpHost: string          // zorunlu
  smtpPort: number          // zorunlu, 1-65535
  smtpUsername: string      // zorunlu
  smtpPassword?: string     // oluştururken zorunlu, güncellerken opsiyonel
  isDefault?: boolean       // default: false
  active?: boolean          // default: true
}
```

---

## Geliştirilmesi Gereken UI Modülü

### Sayfalar

#### `/mail-accounts` — Hesap Listesi
- Tüm hesapları kart veya tablo formatında göster
- Her kart: ad, from adresi, SMTP host:port, varsayılan rozeti, aktif/pasif durumu
- Eylemler: Düzenle, Sil, Varsayılan Yap, Bağlantı Testi
- Sağ üst: "Yeni Hesap Ekle" butonu
- Varsayılan hesap görsel olarak belirtilmeli (rozet, renk vb.)
- Pasif hesaplar soluk görünmeli, "Varsayılan Yap" butonları devre dışı olmalı

#### `/mail-accounts/new` — Yeni Hesap
- Form alanları: ad, from adresi, SMTP host, port, kullanıcı adı, şifre
- Şifre alanı: input type="password", göster/gizle toggle
- "Varsayılan olarak ayarla" checkbox
- Gmail için yönlendirme notu: "Gmail kullanıyorsanız App Password gereklidir"
- Kaydet butonu → POST /api/v1/mail-accounts

#### `/mail-accounts/{id}/edit` — Hesap Düzenle
- Yeni hesap formu ile aynı yapı
- Şifre alanı boş gelir; "Şifreyi değiştirmek için doldurun" placeholder'ı
- Mevcut değerler form'a dolu gelir (şifre hariç)
- Kaydet butonu → PUT /api/v1/mail-accounts/{id}

### Bileşenler

#### `SmtpTestModal`
- "Bağlantı Testi" butonuna tıklanınca açılır
- Tek input: test alıcısı email adresi
- "Test Gönder" butonu → loading state → başarı/hata mesajı
- Hata mesajında SMTP host:port bilgisi görünür

#### `MailAccountSelect` (Form Editörü'nde kullanılacak)
- Dropdown/select bileşeni
- Seçenekler: "Varsayılan (otomatik)" + hesap listesi
- Her seçenekte: hesap adı + from adresi
- Form tanımı kaydedilirken `mailAccountId` gönderilir

---

## UX Akışları

### Varsayılan Hesap Yönetimi
```
Kullanıcı "Varsayılan Yap" tıklar
  → Onay dialog: "Bu hesabı varsayılan yapmak istediğinizden emin misiniz?"
  → PUT /api/v1/mail-accounts/{id}/default
  → Başarı: listeyi yenile, eski varsayılan rozetini kaldır, yenisine ekle
  → Hata (pasif hesap): "Pasif hesap varsayılan yapılamaz" toast
```

### Hesap Silme
```
Kullanıcı "Sil" tıklar
  → Onay dialog: "Bu hesabı silmek istediğinizden emin misiniz?
                  Bu hesabı kullanan formlar varsayılan hesaba geçecektir."
  → DELETE /api/v1/mail-accounts/{id}
  → Başarı: listeden kaldır
```

### Bağlantı Testi
```
Kullanıcı "Test Et" tıklar
  → SmtpTestModal açılır
  → Email girer, "Gönder" tıklar
  → Loading spinner
  → Başarı: yeşil banner "Test maili gönderildi → test@firma.com"
  → Hata: kırmızı banner "SMTP bağlantısı başarısız: ..." (backend hata mesajı)
```

---

## Önemli Güvenlik Notları

1. **Şifre asla gösterilmez:** Response'da `smtpPassword` alanı yoktur.
   Edit formunda şifre alanı her zaman boş başlar.

2. **Şifre maskeleme:** Input `type="password"`, görünürlük toggle opsiyonel.

3. **Şifre güncelleme:** Kullanıcı şifreyi boş bırakırsa backend mevcut şifreyi korur.
   Formu kaydederken şifre alanı boşsa request body'den çıkarın veya `null` gönderin.

4. **Gmail App Password:** 16 karakterli, boşluklar olmadan gönderilmeli.
   UI'da bilgi notu gösterin:
   > "Gmail kullanıyorsanız normal şifre çalışmaz. Google Hesabı → Güvenlik → Uygulama Şifreleri'nden 16 karakterli şifre oluşturun."

---

## Form Editörü Entegrasyonu

Form oluşturma/düzenleme ekranında mail hesabı seçimi:

```
Form Ayarları bölümüne eklenecek alan:
┌─────────────────────────────────────────┐
│ Mail Hesabı                             │
│ ┌─────────────────────────────────────┐ │
│ │ ✉ Varsayılan (otomatik)         ▼  │ │
│ └─────────────────────────────────────┘ │
│ Seçilmezse varsayılan hesap kullanılır  │
└─────────────────────────────────────────┘
```

Seçenek listesi `/api/v1/mail-accounts` → yalnızca `active: true` olanlar.

---

## Backend Hata Kodları

| HTTP | Durum | Sebep |
|------|-------|-------|
| 400  | Bad Request | Validasyon hatası, pasif hesabı varsayılan yapma girişimi |
| 400  | Bad Request | SMTP bağlantı testi başarısız (hata mesajı döner) |
| 404  | Not Found | Hesap bulunamadı |
| 404  | Not Found | Varsayılan hesap tanımlanmamış (EmailQueueService) |
| 409  | Conflict | Veri bütünlüğü ihlali |

Genel response yapısı:
```json
{
  "result": false,
  "message": "Hata açıklaması",
  "data": null
}
```

---

## Geliştirme Öncelik Sırası

1. **Mail Hesabı Listesi** `/mail-accounts` — temel CRUD
2. **Yeni/Düzenle formu** — şifre yönetimi dahil
3. **Bağlantı testi modalı** — SmtpTestModal
4. **Form editörü entegrasyonu** — MailAccountSelect dropdown
5. **Email gönderme ekranı** — varsa mailAccountId seçimi

---

## Backend Repo

- **Repo:** `huseyindol/elly`
- **Branch:** `claude/fix-cors-error-cytcY`
- **İlgili dosyalar:**
  - `src/main/java/com/cms/entity/MailAccount.java`
  - `src/main/java/com/cms/controller/impl/MailAccountController.java`
  - `src/main/java/com/cms/service/impl/MailAccountService.java`
  - `src/main/resources/db-migration-mail-accounts.sql`
