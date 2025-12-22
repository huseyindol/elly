# Migration Script Çalıştırma Kılavuzu

## 📋 Veritabanı Bilgileri

Application properties'den:
- **Host**: `ep-billowing-scene-adbekobg-pooler.c-2.us-east-1.aws.neon.tech`
- **Database**: `neondb`
- **Username**: `neondb_owner`
- **Password**: `npg_NExeW0baq3HB`
- **Schema**: `elly`

---

## 🚀 Yöntem 1: Neon Console SQL Editor (EN KOLAY)

1. **Neon Console'a giriş yapın**: https://console.neon.tech
2. Projenizi seçin
3. Sol menüden **"SQL Editor"** seçin
4. Aşağıdaki SQL'i kopyalayıp yapıştırın:

```sql
-- Token version kolonu ekle
ALTER TABLE users
ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

-- Mevcut kullanıcılar için token_version'ı 0 olarak ayarla (eğer null ise)
UPDATE users SET token_version = 0 WHERE token_version IS NULL;

-- Index ekle (opsiyonel, performans için)
CREATE INDEX IF NOT EXISTS idx_users_token_version ON users (token_version);
```

5. **"Run"** butonuna tıklayın
6. ✅ Migration tamamlandı!

---

## 🖥️ Yöntem 2: psql Komut Satırı (Local)

### Adım 1: psql'in yüklü olduğunu kontrol edin

```bash
psql --version
```

Eğer yüklü değilse:
- **macOS**: `brew install postgresql`
- **Linux**: `sudo apt-get install postgresql-client` (Ubuntu/Debian)
- **Windows**: PostgreSQL installer'dan yükleyin

### Adım 2: Connection String ile bağlanın

```bash
psql "postgresql://xxx:yyy@zzz"
```

### Adım 3: Schema'yı seçin ve migration'ı çalıştırın

```sql
SET search_path TO elly;

-- Token version kolonu ekle
ALTER TABLE users
ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

-- Mevcut kullanıcılar için token_version'ı 0 olarak ayarla (eğer null ise)
UPDATE users SET token_version = 0 WHERE token_version IS NULL;

-- Index ekle (opsiyonel, performans için)
CREATE INDEX IF NOT EXISTS idx_users_token_version ON users (token_version);
```

### Adım 4: Çıkış

```sql
\q
```

---

## 📝 Yöntem 3: Script Dosyasını Direkt Çalıştırma

### psql ile script dosyasını çalıştırma:

```bash
# Önce schema'yı ayarlayın
export PGPASSWORD='npg_NExeW0baq3HB'

psql -h ep-billowing-scene-adbekobg-pooler.c-2.us-east-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb \
     -c "SET search_path TO elly;" \
     -f scripts/migration-add-token-version.sql
```

**Not**: Script dosyasına schema ayarını eklemeniz gerekebilir.

---

## 🔧 Yöntem 4: pgAdmin veya DBeaver (GUI Tool)

1. **pgAdmin** veya **DBeaver** gibi bir GUI tool kullanın
2. Yeni connection oluşturun:
   - **Host**: `ep-billowing-scene-adbekobg-pooler.c-2.us-east-1.aws.neon.tech`
   - **Port**: `5432`
   - **Database**: `neondb`
   - **Username**: `neondb_owner`
   - **Password**: `npg_NExeW0baq3HB`
   - **SSL Mode**: `require`
3. `elly` schema'sını seçin
4. SQL Editor'ü açın
5. `scripts/migration-add-token-version.sql` dosyasının içeriğini yapıştırın
6. Çalıştırın

---

## ✅ Migration Sonrası Kontrol

Migration'ın başarılı olduğunu kontrol etmek için:

```sql
-- Schema'yı seç
SET search_path TO elly;

-- Kolonun eklendiğini kontrol et
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_schema = 'elly' 
  AND table_name = 'users' 
  AND column_name = 'token_version';

-- Mevcut kullanıcıların token_version değerlerini kontrol et
SELECT id, username, email, token_version 
FROM users;
```

---

## 🐛 Sorun Giderme

### Hata: "column already exists"
- Kolon zaten eklenmiş, sorun yok. Devam edebilirsiniz.

### Hata: "permission denied"
- Kullanıcı yetkilerini kontrol edin. Neon'da owner kullanıcısı genellikle tüm yetkilere sahiptir.

### Hata: "schema does not exist"
- Schema adını kontrol edin: `elly`
- Veya schema oluşturun: `CREATE SCHEMA IF NOT EXISTS elly;`

---

## 📚 İlgili Dosyalar

- Migration Script: `scripts/migration-add-token-version.sql`
- User Entity: `src/main/java/com/cms/entity/User.java`
