---
description: "Yeni veritabanı migrasyonu oluşturur veya mevcut SQL'i gözden geçirir"
argument-hint: "<migration-konusu>"
allowed-tools: Read, Write, Glob, Grep
---

Elly'nin PostgreSQL multi-tenant yapısı için `$ARGUMENTS` konusunda migration yaz.

Önce `src/main/resources/migration/*.sql` dosyalarını incele ve mevcut migration stilini takip et.

**Kurallar:**
- Doğru dizin: `src/main/resources/migration/db-migration-<konu>.sql`
- Her SQL bloğunu `-- Migration: <açıklama>` yorumuyla belge
- Idempotent ol: `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `DROP TABLE IF EXISTS`
- Rollback script'i de yaz (yorum olarak)
- İlgili index önerilerini ekle (`CREATE INDEX IF NOT EXISTS`)
- Foreign key constraint'leri için naming convention: `fk_<tablo>_<referans>`

## KAPSAM KURALI (kritik)

Migration'ın hangi DB'lerde çalıştırılacağı, **entity'nin nerede sorgulandığına** bağlı değildir — **entity'nin nerede tanımlı olduğuna** bağlıdır.

| Senaryo | Çalıştırılacak DB'ler |
|---------|------------------------|
| Tablo/kolon `com.cms.entity.*` altında ve **tüm tenant'larda** sorgulanabilir (User, MailAccount vb.) | **basedb + tenant1 + tenant2** |
| Tablo/kolon sadece chat (basedb-only routing) | basedb |
| Tablo/kolon sadece bir tenant'ta yaşıyor (özel durum) | İlgili tenant |

**Neden:** Hibernate `hbm2ddl.auto=validate` modunda sadece basedb'i kontrol eder. AMA runtime'da herhangi bir tenant context'inde aynı entity sorgulanırsa Hibernate `SELECT *, yeni_kolon FROM tablo` üretir; kolon yoksa SQL hatası fırlatır.

**Tipik hata:** Sadece basedb'de migration çalıştırılır → startup başarılı → tenant1 üzerinden istek gelince runtime SQL hatası (`column does not exist`). Bunu önle.

## Çalıştırma — kubectl ile

```bash
# YEREL SQL dosyası → pod içinde psql (pipe, -i kullan, -it değil; -f local file için ÇALIŞMAZ)
kubectl exec -i postgres-basedb-0  -n elly -- psql -U postgres -d elly_basedb  < db-migration-X.sql
kubectl exec -i postgres-tenant1-0 -n elly -- psql -U postgres -d elly_tenant1 < db-migration-X.sql
kubectl exec -i postgres-tenant2-0 -n elly -- psql -U postgres -d elly_tenant2 < db-migration-X.sql
```

**Çıktı:**
1. Migration SQL dosyası (hazır, çalıştırılabilir)
2. Rollback SQL (yorum olarak)
3. **Çalıştırılacak DB listesi** (kapsam kuralına göre — gerekçeli)
4. Yukarıdaki kubectl komutlarının hazır kopyala-yapıştır blokları
5. Uygulanacak sıra (bağımlılıklar varsa)
