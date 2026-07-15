# Kullanıcı Yönetimi — Panel Entegrasyon Kılavuzu

Elly CMS'de iki kullanıcı tipi vardır; **her ikisi de kendi tenant DB'sinde yaşar**
(basedb KALDIRILDI — tamamen tenant-scoped mimari):

| Tip | DB | Login Türü | Kullanım |
|-----|----|-----------|---------|
| **Panel admin** | kendi tenant DB'si (`tenant1`, `tenant2`, ...) | `loginType: "admin"` | CMS panelini yönetenler (SUPER_ADMIN/ADMIN/EDITOR/VIEWER rolü) |
| **Tenant site kullanıcısı** | kendi tenant DB'si | `loginType: "tenant"` | Sitenin son kullanıcıları (TENANT rolü) |

Tenant, JWT `tenantId` claim'inden veya `/api/v1/public/{tid}` URL'inden çözülür; `tenantId` zorunludur.

---

## 1. Kimlik Doğrulama (Auth)

### Panel Admin Girişi → kendi tenant DB'si
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "huseyindol",
  "password": "112233",
  "tenantId": "tenant1",
  "loginType": "admin"
}
```

**Response:**
```json
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 1,
  "username": "huseyindol",
  "roles": ["SUPER_ADMIN"],
  "permissions": ["users:manage", "roles:read", ...]
}
```

> Dönen token'ı tüm panel isteklerinde `Authorization: Bearer {token}` olarak kullanın.

---

### Tenant Site Kullanıcısı Girişi → tenant1 DB
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "ali@site.com",
  "password": "123456",
  "tenantId": "tenant1",
  "loginType": "tenant"
}
```

---

### Tenant Site Kullanıcısı Kayıt → tenant1 DB
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "ali",
  "email": "ali@site.com",
  "password": "123456",
  "firstName": "Ali",
  "lastName": "Yılmaz",
  "tenantId": "tenant1"
}
```

> `tenantId` ZORUNLUDUR — verilmezse istek `TENANT_REQUIRED` ile reddedilir (basedb fallback'i yok).
> Register her zaman TENANT rolü atar; admin kullanıcı yalnız panelden `POST /api/v1/users` ile oluşturulur.

---

## 2. Panel Admin Kullanıcılarını Yönetme (kendi tenant DB'sinde)

**Gerekli:** `Authorization: Bearer {admin_token}` + `users:manage` yetkisi

### Tüm Admin Kullanıcılarını Listele
```http
GET /api/v1/users
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "username": "huseyindol",
      "email": "huseyindol@gmail.com",
      "firstName": "Hüseyin",
      "lastName": "Dol",
      "provider": "local",
      "isActive": true,
      "managedTenants": ["tenant1", "tenant2"],
      "roles": ["SUPER_ADMIN"],
      "createdAt": "2026-04-01T10:00:00"
    }
  ]
}
```

### Tek Admin Kullanıcı Getir
```http
GET /api/v1/users/{id}
Authorization: Bearer {admin_token}
```

### Kendi Profilini Güncelle (giriş yapan admin)
```http
PUT /api/v1/users/me
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "firstName": "Hüseyin",
  "lastName": "Dol",
  "email": "yeni@email.com"
}
```

---

## 3. Tenant Kullanıcılarını Yönetme (tenant1, tenant2, ...)

**Gerekli:** Admin JWT (`loginType: "admin"`) + `users:manage` yetkisi

> Tüm endpoint'ler `AdminLoginInterceptor` tarafından korunur — sadece admin panel token'ı ile çalışır.

### Yeni Tenant Kullanıcısı Oluştur
```http
POST /api/v1/admin/tenants/{tenantId}/users
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "username": "ali",
  "email": "ali@tenant1.com",
  "password": "Gizli123!",
  "firstName": "Ali",
  "lastName": "Yılmaz"
}
```

**Response:** `201 Created`
```json
{
  "result": true,
  "data": {
    "id": 5,
    "username": "ali",
    "email": "ali@tenant1.com",
    "firstName": "Ali",
    "lastName": "Yılmaz",
    "isActive": true,
    "provider": "local",
    "roles": [],
    "createdAt": "2026-04-29T12:00:00"
  }
}
```

### Tenant Kullanıcılarını Listele
```http
GET /api/v1/admin/tenants/tenant1/users
Authorization: Bearer {admin_token}
```

### Tek Tenant Kullanıcısı Getir
```http
GET /api/v1/admin/tenants/tenant1/users/{id}
Authorization: Bearer {admin_token}
```

### Tenant Kullanıcısını Güncelle
```http
PUT /api/v1/admin/tenants/tenant1/users/{id}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "firstName": "Ali",
  "lastName": "Kaya",
  "email": "ali.kaya@tenant1.com",
  "isActive": true
}
```

> Sadece gönderilen alanlar güncellenir. Boş bırakılanlar değişmez.

### Tenant Kullanıcısını Sil
```http
DELETE /api/v1/admin/tenants/tenant1/users/{id}
Authorization: Bearer {admin_token}
```

**Response:** `204 No Content`

### Tenant Kullanıcısını Aktif / Pasif Yap
```http
PATCH /api/v1/admin/tenants/tenant1/users/{id}/status?isActive=false
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 5,
    "username": "ali",
    "isActive": false,
    ...
  }
}
```

---

## 4. Rol Yönetimi

> Roller her tenant'ın KENDİ DB'sinde tutulur (basedb yok). Panel rolleri: SUPER_ADMIN/ADMIN/EDITOR/VIEWER;
> tenant-site kullanıcıları izinsiz "tag" rolü TENANT taşır (TENANT rolü münhasırdır — panel rolleriyle karışmaz).

### Tüm Rolleri Listele
```http
GET /api/v1/roles
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "result": true,
  "data": [
    { "id": 1, "name": "SUPER_ADMIN", "permissions": [...] },
    { "id": 2, "name": "ADMIN", "permissions": [...] },
    { "id": 3, "name": "EDITOR", "permissions": [...] },
    { "id": 4, "name": "VIEWER", "permissions": [...] }
  ]
}
```

### Tüm İzinleri Listele
```http
GET /api/v1/roles/permissions
Authorization: Bearer {admin_token}
```

### Modüle Göre İzinleri Listele
```http
GET /api/v1/roles/permissions/module/posts
Authorization: Bearer {admin_token}
```

> Modül adları: `posts`, `pages`, `components`, `widgets`, `banners`, `assets`, `comments`, `forms`, `ratings`, `contents`, `basic_infos`, `mail`, `emails`, `cache`, `tenants`, `users`, `roles`, `rabbit`, `email_templates`

### Admin Kullanıcısına Rol Ata
```http
PUT /api/v1/roles/users/{userId}/roles
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "roleIds": [2, 3]
}
```

> `roleIds` tam listedir — mevcut roller silinip bu liste atanır.

### Yeni Rol Oluştur
```http
POST /api/v1/roles
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "name": "CONTENT_MANAGER",
  "description": "İçerik yöneticisi"
}
```

### Role İzin Ata
```http
PUT /api/v1/roles/{roleId}/permissions
Authorization: Bearer {admin_token}
Content-Type: application/json

[1, 2, 3, 5, 7]
```

> `permissionIds` tam listedir — mevcut izinler silinip bu liste atanır.

---

## 5. Mevcut Kullanıcının Yetkilerini Sorgula

Panel'de hangi menülerin gösterileceğini belirlemek için kullanın.

```http
GET /api/v1/users/me/permissions
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "result": true,
  "data": {
    "roles": ["SUPER_ADMIN"],
    "permissions": ["posts:create", "posts:read", "posts:update", ...],
    "permissionsByModule": {
      "posts": ["create", "read", "update", "delete"],
      "pages": ["create", "read", "update", "delete"],
      "users": ["manage"],
      ...
    }
  }
}
```

---

## 6. Varsayılan Roller ve İzinler

| Rol | Açıklama | Anahtar İzinler |
|-----|---------|-----------------|
| `SUPER_ADMIN` | Tüm yetkiler | Hepsi (40+) |
| `ADMIN` | Yönetici | `roles:*` ve `users:manage` hariç hepsi |
| `EDITOR` | İçerik editörü | posts, pages, components, widgets, banners, assets, comments, forms, ratings, contents, basic_infos |
| `VIEWER` | Salt okuma | Tüm `*:read` izinleri |

---

## 7. Sık Kullanılan İş Akışları

### Panel'de yeni admin ekle
```
1. POST /api/v1/users  (SUPER_ADMIN yetkisiyle)
   → { username, email, password, audience: "panel", roleIds: [2] }
   Register ile admin OLUŞTURULAMAZ (register her zaman TENANT rolü atar).
```

### Panel'den tenant1'e yeni site kullanıcısı ekle
```
1. POST /api/v1/admin/tenants/tenant1/users  → { username, email, password, firstName, lastName }
2. Başarı → kullanıcı tenant1 DB'sine kayıt edildi, panel listesinde görünür
```

### Tenant kullanıcısını pasif yap (hesap askıya al)
```
PATCH /api/v1/admin/tenants/tenant1/users/{id}/status?isActive=false
```

### Admin kullanıcısını pasif yap
```
PUT /api/v1/admin/tenants/tenant1/users/{id}   → { "isActive": false }
```

---

## 8. Hata Kodları

| HTTP | errorCode | Açıklama |
|------|-----------|---------|
| 401 | `BAD_CREDENTIALS` | Token geçersiz veya süresi dolmuş |
| 403 | `ACCESS_DENIED` | Yetersiz yetki (permission yok) |
| 403 | `FORBIDDEN` | Admin token gerektiren endpoint'e tenant token ile erişim |
| 404 | `RESOURCE_NOT_FOUND` | Kullanıcı veya tenant bulunamadı |
| 409 | `CONFLICT` | Username veya email zaten kullanımda |
| 400 | `BAD_REQUEST` | Geçersiz tenantId veya eksik alan |
