# Kullanıcı Yönetimi — Panel Entegrasyon Kılavuzu

Elly CMS'de iki ayrı kullanıcı tipi ve iki ayrı DB vardır:

| Tip | DB | Login Türü | Kullanım |
|-----|----|-----------|---------|
| **Panel admin** | `basedb` | `loginType: "admin"` | CMS panelini yönetenler |
| **Tenant site kullanıcısı** | `tenant1`, `tenant2`, ... | `loginType: "tenant"` | Sitenin son kullanıcıları |

---

## 1. Kimlik Doğrulama (Auth)

### Panel Admin Girişi → basedb
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "huseyindol",
  "password": "112233",
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

> `tenantId` verilmezse kullanıcı basedb'ye kaydedilir.

---

## 2. Panel Admin Kullanıcılarını Yönetme (basedb)

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

## 4. Rol Yönetimi (basedb — Admin Kullanıcıları için)

> Roller basedb'de tutulur. Tenant kullanıcıları için rol atama şu an desteklenmiyor.

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
1. POST /api/auth/register   → { username, email, password }   (tenantId yok → basedb)
2. GET  /api/v1/users        → yeni kullanıcının id'sini bul
3. PUT  /api/v1/roles/users/{id}/roles  → { "roleIds": [2] }  (ADMIN rolü ver)
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
