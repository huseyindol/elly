# Elly CMS — Panel (Next.js) İçin RBAC Permission Sistemi Rehberi

Bu doküman, Elly CMS backend'indeki Rol Tabanlı Erişim Kontrolü (RBAC) sistemini ve bu sistemin Next.js panel projesinde nasıl kullanılması gerektiğini açıklar.

> **Hedef:** Panel'de her kullanıcıya yalnızca yetkisi olan menüleri, sayfaları ve butonları göstermek.

---

## 1. Backend API Endpoint'leri

### 1.1 Login — `POST /api/v1/auth/login`

Login response'unda kullanıcının rolleri ve izinleri doğrudan döner. **Ek API çağrısı gerekmez.**

**Request:**
```json
{
  "usernameOrEmail": "admin",
  "password": "123456",
  "tenantId": "basedb",
  "loginType": "admin"
}
```

**Response:**
```json
{
  "result": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "username": "admin",
    "email": "admin@example.com",
    "userCode": "AD",
    "expiredDate": 1714230000000,
    "roles": ["SUPER_ADMIN"],
    "permissions": [
      "assets:create",
      "assets:delete",
      "assets:read",
      "assets:update",
      "banners:create",
      "banners:delete",
      "banners:read",
      "banners:update",
      "posts:create",
      "posts:read",
      "posts:update",
      "posts:delete",
      "..."
    ]
  }
}
```

> `roles` ve `permissions` alanları `refreshToken` response'unda da aynı formatta döner.

---

### 1.2 Kullanıcı İzinleri — `GET /api/v1/users/me/permissions`

Kullanıcının güncel izinlerini detaylı şekilde döner. Rol değişikliklerinden sonra güncel veriyi almak için bu endpoint kullanılır.

**Header:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "result": true,
  "data": {
    "roles": ["SUPER_ADMIN"],
    "permissions": [
      "assets:create",
      "assets:delete",
      "assets:read",
      "assets:update",
      "banners:create",
      "banners:delete",
      "banners:read",
      "banners:update",
      "basic_infos:create",
      "basic_infos:delete",
      "basic_infos:read",
      "basic_infos:update",
      "cache:manage",
      "cache:read",
      "comments:create",
      "comments:delete",
      "comments:read",
      "components:create",
      "components:delete",
      "components:read",
      "components:update",
      "contents:create",
      "contents:delete",
      "contents:read",
      "contents:update",
      "email_templates:manage",
      "email_templates:read",
      "emails:read",
      "emails:retry",
      "emails:send",
      "forms:create",
      "forms:delete",
      "forms:read",
      "forms:submit",
      "forms:update",
      "mail:create",
      "mail:delete",
      "mail:read",
      "mail:update",
      "pages:create",
      "pages:delete",
      "pages:read",
      "pages:update",
      "posts:create",
      "posts:delete",
      "posts:read",
      "posts:update",
      "rabbit:manage",
      "rabbit:read",
      "ratings:create",
      "ratings:read",
      "roles:create",
      "roles:delete",
      "roles:read",
      "roles:update",
      "tenants:manage",
      "tenants:read",
      "users:manage",
      "users:read",
      "users:update",
      "widgets:create",
      "widgets:delete",
      "widgets:read",
      "widgets:update"
    ],
    "permissionsByModule": {
      "assets": ["create", "delete", "read", "update"],
      "banners": ["create", "delete", "read", "update"],
      "basic_infos": ["create", "delete", "read", "update"],
      "cache": ["manage", "read"],
      "comments": ["create", "delete", "read"],
      "components": ["create", "delete", "read", "update"],
      "contents": ["create", "delete", "read", "update"],
      "email_templates": ["manage", "read"],
      "emails": ["read", "retry", "send"],
      "forms": ["create", "delete", "read", "submit", "update"],
      "mail": ["create", "delete", "read", "update"],
      "pages": ["create", "delete", "read", "update"],
      "posts": ["create", "delete", "read", "update"],
      "rabbit": ["manage", "read"],
      "ratings": ["create", "read"],
      "roles": ["create", "delete", "read", "update"],
      "tenants": ["manage", "read"],
      "users": ["manage", "read", "update"],
      "widgets": ["create", "delete", "read", "update"]
    }
  }
}
```

---

## 2. Permission Formatı

Her permission `modül:işlem` formatındadır. Örnek: `posts:create`, `pages:read`, `cache:manage`

### 2.1 Tüm Modüller ve İşlemleri

| # | Modül | İşlemler | Panel Karşılığı |
|---|-------|----------|-----------------|
| 1 | `posts` | `create`, `read`, `update`, `delete` | Blog yazıları yönetimi |
| 2 | `pages` | `create`, `read`, `update`, `delete` | Sayfa yönetimi |
| 3 | `components` | `create`, `read`, `update`, `delete` | Bileşen yönetimi |
| 4 | `widgets` | `create`, `read`, `update`, `delete` | Widget yönetimi |
| 5 | `banners` | `create`, `read`, `update`, `delete` | Banner yönetimi |
| 6 | `assets` | `create`, `read`, `update`, `delete` | Medya dosyaları |
| 7 | `comments` | `create`, `read`, `delete` | Yorum yönetimi (update yok) |
| 8 | `forms` | `create`, `read`, `update`, `delete`, `submit` | Form yönetimi |
| 9 | `ratings` | `create`, `read` | Puanlama (sadece oku/oluştur) |
| 10 | `contents` | `create`, `read`, `update`, `delete` | CMS içerik yönetimi |
| 11 | `basic_infos` | `create`, `read`, `update`, `delete` | Temel bilgi yönetimi |
| 12 | `mail` | `create`, `read`, `update`, `delete` | Mail hesap ayarları |
| 13 | `emails` | `send`, `read`, `retry` | E-posta gönderim yönetimi |
| 14 | `email_templates` | `read`, `manage` | E-posta şablon yönetimi |
| 15 | `cache` | `read`, `manage` | Redis cache yönetimi |
| 16 | `tenants` | `read`, `manage` | Tenant yönetimi |
| 17 | `users` | `read`, `update`, `manage` | Kullanıcı yönetimi |
| 18 | `roles` | `create`, `read`, `update`, `delete` | Rol yönetimi |
| 19 | `rabbit` | `read`, `manage` | RabbitMQ admin |

### 2.2 Varsayılan Roller

| Rol | Açıklama | İzin Kapsamı |
|-----|----------|-------------|
| `SUPER_ADMIN` | Tam yetki | Tüm izinler (~47 permission) |
| `ADMIN` | Panel yönetimi | `roles:*` ve `users:manage` hariç tümü |
| `EDITOR` | İçerik editörü | Sadece içerik modülleri: posts, pages, components, widgets, banners, assets, comments, forms, ratings, contents, basic_infos |
| `VIEWER` | Sadece okuma | Tüm modüllerin sadece `:read` izinleri |

---

## 3. Next.js Panel Entegrasyonu

### 3.1 TypeScript Tip Tanımları

```typescript
// types/permissions.ts

/** Login veya /me/permissions response'undan gelen izin verisi */
export interface UserPermissions {
  roles: string[];
  permissions: string[];
  permissionsByModule: Record<string, string[]>;
}

/** Backend'deki modül isimleri — birebir eşleşmeli */
export const MODULES = {
  POSTS: 'posts',
  PAGES: 'pages',
  COMPONENTS: 'components',
  WIDGETS: 'widgets',
  BANNERS: 'banners',
  ASSETS: 'assets',
  COMMENTS: 'comments',
  FORMS: 'forms',
  RATINGS: 'ratings',
  CONTENTS: 'contents',
  BASIC_INFOS: 'basic_infos',
  MAIL: 'mail',
  EMAILS: 'emails',
  EMAIL_TEMPLATES: 'email_templates',
  CACHE: 'cache',
  TENANTS: 'tenants',
  USERS: 'users',
  ROLES: 'roles',
  RABBIT: 'rabbit',
} as const;

export type ModuleKey = keyof typeof MODULES;
export type ModuleValue = (typeof MODULES)[ModuleKey];
export type Action = 'create' | 'read' | 'update' | 'delete' | 'manage' | 'send' | 'retry' | 'submit';
```

### 3.2 Permission Hook

```typescript
// hooks/usePermission.ts
'use client';

import { useAuth } from '@/contexts/AuthContext'; // veya kendi auth store'un

export function usePermission() {
  const { user } = useAuth();
  const permissions: string[] = user?.permissions ?? [];
  const roles: string[] = user?.roles ?? [];

  /** Tek bir izni kontrol et: hasPermission('posts:create') */
  const hasPermission = (permission: string): boolean => {
    return permissions.includes(permission);
  };

  /** Verilen izinlerden en az birinin olup olmadığını kontrol et */
  const hasAnyPermission = (...perms: string[]): boolean => {
    return perms.some((p) => permissions.includes(p));
  };

  /** Verilen tüm izinlerin olup olmadığını kontrol et */
  const hasAllPermissions = (...perms: string[]): boolean => {
    return perms.every((p) => permissions.includes(p));
  };

  /** Modül + aksiyon bazlı kontrol: canAccess('posts', 'create') */
  const canAccess = (module: string, action: Action): boolean => {
    return permissions.includes(`${module}:${action}`);
  };

  /** Modüle herhangi bir erişim var mı? (sidebar menü gösterimi için) */
  const canAccessModule = (module: string): boolean => {
    return permissions.some((p) => p.startsWith(`${module}:`));
  };

  /** Rol kontrolü: hasRole('SUPER_ADMIN') */
  const hasRole = (role: string): boolean => {
    return roles.includes(role);
  };

  /** Süper admin mi? */
  const isSuperAdmin = (): boolean => {
    return roles.includes('SUPER_ADMIN');
  };

  return {
    permissions,
    roles,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    canAccess,
    canAccessModule,
    hasRole,
    isSuperAdmin,
  };
}
```

### 3.3 PermissionGate Bileşeni

İzin gerektiren herhangi bir UI elemanını sarmalar. İzin yoksa o elemanı render etmez.

```tsx
// components/PermissionGate.tsx
'use client';

import { usePermission } from '@/hooks/usePermission';
import type { Action } from '@/types/permissions';

interface PermissionGateProps {
  /** Direkt permission string: 'posts:create' */
  permission?: string;
  /** Modül adı ('posts', 'pages', ...) — action ile birlikte kullanılır */
  module?: string;
  /** Aksiyon ('create', 'read', ...) — module ile birlikte kullanılır */
  action?: Action;
  /** Bu izinlerden herhangi birine sahipse göster */
  any?: string[];
  /** İzin yoksa gösterilecek alternatif içerik */
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

export function PermissionGate({
  permission,
  module,
  action,
  any,
  fallback = null,
  children,
}: PermissionGateProps) {
  const { hasPermission, canAccess, hasAnyPermission, isSuperAdmin } = usePermission();

  // SUPER_ADMIN her şeyi görür
  if (isSuperAdmin()) return <>{children}</>;

  let allowed = false;

  if (permission) {
    allowed = hasPermission(permission);
  } else if (module && action) {
    allowed = canAccess(module, action);
  } else if (any && any.length > 0) {
    allowed = hasAnyPermission(...any);
  }

  return allowed ? <>{children}</> : <>{fallback}</>;
}
```

---

## 4. Kullanım Örnekleri

### 4.1 Sidebar Menü

```tsx
// components/Sidebar.tsx
import { PermissionGate } from '@/components/PermissionGate';
import { MODULES } from '@/types/permissions';

export function Sidebar() {
  return (
    <nav>
      {/* İçerik Yönetimi */}
      <PermissionGate module={MODULES.POSTS} action="read">
        <SidebarItem href="/posts" icon={FileText} label="Yazılar" />
      </PermissionGate>

      <PermissionGate module={MODULES.PAGES} action="read">
        <SidebarItem href="/pages" icon={Layout} label="Sayfalar" />
      </PermissionGate>

      <PermissionGate module={MODULES.CONTENTS} action="read">
        <SidebarItem href="/contents" icon={Database} label="İçerikler" />
      </PermissionGate>

      <PermissionGate module={MODULES.BASIC_INFOS} action="read">
        <SidebarItem href="/basic-infos" icon={Info} label="Temel Bilgiler" />
      </PermissionGate>

      {/* Bileşenler */}
      <PermissionGate module={MODULES.COMPONENTS} action="read">
        <SidebarItem href="/components" icon={Puzzle} label="Bileşenler" />
      </PermissionGate>

      <PermissionGate module={MODULES.WIDGETS} action="read">
        <SidebarItem href="/widgets" icon={Grid} label="Widget'lar" />
      </PermissionGate>

      <PermissionGate module={MODULES.BANNERS} action="read">
        <SidebarItem href="/banners" icon={Image} label="Banner'lar" />
      </PermissionGate>

      {/* Medya */}
      <PermissionGate module={MODULES.ASSETS} action="read">
        <SidebarItem href="/assets" icon={Upload} label="Medya" />
      </PermissionGate>

      {/* Etkileşim */}
      <PermissionGate module={MODULES.COMMENTS} action="read">
        <SidebarItem href="/comments" icon={MessageSquare} label="Yorumlar" />
      </PermissionGate>

      <PermissionGate module={MODULES.FORMS} action="read">
        <SidebarItem href="/forms" icon={ClipboardList} label="Formlar" />
      </PermissionGate>

      <PermissionGate module={MODULES.RATINGS} action="read">
        <SidebarItem href="/ratings" icon={Star} label="Puanlamalar" />
      </PermissionGate>

      {/* İletişim */}
      <PermissionGate module={MODULES.MAIL} action="read">
        <SidebarItem href="/mail" icon={Mail} label="Mail Hesapları" />
      </PermissionGate>

      <PermissionGate module={MODULES.EMAILS} action="read">
        <SidebarItem href="/emails" icon={Send} label="E-postalar" />
      </PermissionGate>

      <PermissionGate module={MODULES.EMAIL_TEMPLATES} action="read">
        <SidebarItem href="/email-templates" icon={FileCode} label="E-posta Şablonları" />
      </PermissionGate>

      {/* Sistem — Sadece Admin */}
      <PermissionGate module={MODULES.USERS} action="read">
        <SidebarItem href="/users" icon={Users} label="Kullanıcılar" />
      </PermissionGate>

      <PermissionGate module={MODULES.ROLES} action="read">
        <SidebarItem href="/roles" icon={Shield} label="Roller" />
      </PermissionGate>

      <PermissionGate module={MODULES.TENANTS} action="read">
        <SidebarItem href="/tenants" icon={Globe} label="Tenant'lar" />
      </PermissionGate>

      <PermissionGate module={MODULES.CACHE} action="read">
        <SidebarItem href="/cache" icon={Zap} label="Cache" />
      </PermissionGate>

      <PermissionGate module={MODULES.RABBIT} action="read">
        <SidebarItem href="/rabbit" icon={Server} label="RabbitMQ" />
      </PermissionGate>
    </nav>
  );
}
```

### 4.2 Butonlarda İzin Kontrolü

```tsx
// Listeleme sayfası — oluşturma butonu
<PermissionGate module="posts" action="create">
  <Button onClick={() => router.push('/posts/new')}>
    Yeni Yazı Oluştur
  </Button>
</PermissionGate>

// Tablo satırında — düzenleme ve silme
<PermissionGate module="posts" action="update">
  <Button variant="ghost" onClick={() => handleEdit(post.id)}>
    Düzenle
  </Button>
</PermissionGate>

<PermissionGate module="posts" action="delete">
  <Button variant="destructive" onClick={() => handleDelete(post.id)}>
    Sil
  </Button>
</PermissionGate>
```

### 4.3 Hook ile Programatik Kontrol

```tsx
function PostsPage() {
  const { canAccess, isSuperAdmin } = usePermission();

  // Kolon tanımında kullanım
  const columns = [
    { key: 'title', label: 'Başlık' },
    { key: 'status', label: 'Durum' },
    // Sadece update veya delete izni varsa aksiyon kolonu göster
    ...(canAccess('posts', 'update') || canAccess('posts', 'delete')
      ? [{ key: 'actions', label: 'İşlemler' }]
      : []),
  ];

  // Form submit öncesi kontrol
  const handleSubmit = () => {
    if (!canAccess('posts', 'create')) {
      toast.error('Bu işlem için yetkiniz yok');
      return;
    }
    // ... submit logic
  };
}
```

### 4.4 Sayfa Düzeyinde Koruma (Layout veya Page)

```tsx
// app/(dashboard)/roles/page.tsx
'use client';

import { usePermission } from '@/hooks/usePermission';
import { redirect } from 'next/navigation';

export default function RolesPage() {
  const { canAccess } = usePermission();

  if (!canAccess('roles', 'read')) {
    redirect('/unauthorized');
  }

  return <RolesContent />;
}
```

---

## 5. Auth Akışı ve İzin Yönetimi

```
1. Kullanıcı login olur → POST /api/v1/auth/login
2. Response'dan roles[] ve permissions[] alınır
3. Bu veriler state'e (Context, Zustand, vb.) yazılır
4. Tüm PermissionGate ve usePermission bu store'dan okur
5. Token yenilendiğinde (refresh) → güncel roles/permissions gelir
6. Rol değişikliği olursa → GET /api/v1/users/me/permissions ile güncelle
```

### Login Sonrası Store'a Yazma

```typescript
// Login handler
const handleLogin = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  const { token, refreshToken, roles, permissions, ...userData } = response.data;

  // Token'ları kaydet
  setTokens(token, refreshToken);

  // Kullanıcı bilgilerini ve izinleri store'a yaz
  setUser({
    ...userData,
    roles,
    permissions,
  });
};
```

### İzinleri Güncelleme (Rol değişikliği sonrası)

```typescript
const refreshPermissions = async () => {
  const response = await api.get('/users/me/permissions');
  const { roles, permissions, permissionsByModule } = response.data;
  updatePermissions({ roles, permissions, permissionsByModule });
};
```

---

## 6. Sidebar Menü Gruplaması Önerisi

Panel sidebar'ını aşağıdaki gibi gruplamak UX açısından mantıklıdır:

```
📝 İçerik Yönetimi
   ├── Yazılar          → posts:read
   ├── Sayfalar         → pages:read
   ├── İçerikler        → contents:read
   └── Temel Bilgiler   → basic_infos:read

🧩 Bileşenler
   ├── Bileşenler       → components:read
   ├── Widget'lar       → widgets:read
   └── Banner'lar       → banners:read

📸 Medya
   └── Dosyalar         → assets:read

💬 Etkileşim
   ├── Yorumlar         → comments:read
   ├── Formlar          → forms:read
   └── Puanlamalar      → ratings:read

📧 İletişim
   ├── Mail Hesapları   → mail:read
   ├── E-postalar       → emails:read
   └── Şablonlar        → email_templates:read

⚙️ Sistem (Sadece Admin/SuperAdmin)
   ├── Kullanıcılar     → users:read
   ├── Roller           → roles:read
   ├── Tenant'lar       → tenants:read
   ├── Cache            → cache:read
   └── RabbitMQ         → rabbit:read
```

> Her grup başlığı da ilk çocuğunun izniyle kontrol edilebilir. Gruptaki hiçbir öğeye erişim yoksa grup başlığı da gizlenmelidir.

---

## 7. Önemli Notlar

1. **Backend'de her API çağrısı da korumalıdır.** Panel'de butonu gizlemek yeterli değil; backend'de `@PreAuthorize` ile her endpoint korunuyor. Yetkisiz API çağrısı yapılırsa backend `403 Forbidden` döner.

2. **SUPER_ADMIN tüm izinlere sahiptir.** Panel'de `isSuperAdmin()` kontrolü ile bu rolü ayrıcalıklı tutabilirsin.

3. **Permission'lar Redis'te cache'lenir** (30 dk TTL). Rol değişikliği yapıldığında backend cache'i otomatik temizler, ancak kullanıcının panel'deki store'u eski izinlerle kalabilir. Rol değişikliği sonrası `GET /users/me/permissions` çağrılmalıdır.

4. **Backend 403 hatası geldiğinde** panel'in bu hatayı yakalaması ve kullanıcıya "Yetkiniz yok" mesajı göstermesi gerekir. Axios interceptor ile global olarak handle edilebilir:

```typescript
// lib/api.ts — Axios interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403) {
      toast.error('Bu işlem için yetkiniz bulunmuyor.');
      // İsteğe bağlı: izinleri yeniden çek
      refreshPermissions();
    }
    return Promise.reject(error);
  }
);
```

5. **Yeni register olan kullanıcılara otomatik rol atanmaz.** Admin panelden manuel rol ataması yapılmalıdır. Register response'unda `roles` ve `permissions` boş dizi olarak döner.
