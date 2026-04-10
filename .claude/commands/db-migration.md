---
description: "Yeni veritabanı migrasyonu oluşturur veya mevcut SQL'i gözden geçirir"
argument-hint: "<migration-konusu>"
allowed-tools: Read, Write, Glob, Grep
---

Elly'nin PostgreSQL multi-tenant yapısı için `$ARGUMENTS` konusunda migration yaz.

Önce `src/main/resources/*.sql` dosyalarını incele ve mevcut migration stilini takip et.

**Kurallar:**
- 3 veritabanı için de çalışmalı: `elly_basedb`, `elly_tenant1`, `elly_tenant2`
- Yeni dosya adı: `src/main/resources/db-migration-<konu>.sql`
- Her SQL bloğunu `-- Migration: <açıklama>` yorumuyla belge
- Idempotent ol: `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `DROP TABLE IF EXISTS`
- Rollback script'i de yaz (yorum olarak)
- İlgili index önerilerini ekle (`CREATE INDEX IF NOT EXISTS`)
- Foreign key constraint'leri için naming convention: `fk_<tablo>_<referans>`

**Çıktı:**
1. Migration SQL dosyası (hazır, çalıştırılabilir)
2. Rollback SQL (yorum olarak)
3. Etkilenen tenant'ların listesi
4. Uygulanacak sıra (bağımlılıklar varsa)
