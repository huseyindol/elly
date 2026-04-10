---
description: "Redis cache kullanımını inceler, tutarsızlıkları ve optimizasyon fırsatlarını bulur"
allowed-tools: Read, Grep, Glob
---

Elly'deki Redis cache kullanımını tam olarak denetle.

Önce `src/main/java/com/cms/config/CacheConfig.java` dosyasını oku, sonra tüm `@Cacheable`, `@CacheEvict`, `@CachePut` kullanımlarını tara.

**Kontrol Listesi:**

Tenant İzolasyonu:
- [ ] Cache key'lerinde `tenantId` var mı? (`@Cacheable(key = "#tenantId + ':' + #id")`)
- [ ] Farklı tenant'ların aynı cache key'ini paylaşma riski?

Tutarlılık:
- [ ] Her `@Cacheable` için karşılık gelen `@CacheEvict` var mı?
- [ ] `allEntries = true` gereksiz yere kullanılıyor mu? (performans etkisi)
- [ ] Cache adı tutarlı mı? (typo, büyük/küçük harf)

Konfigürasyon:
- [ ] `CacheConfig.java`'daki TTL değerleri mantıklı mı?
- [ ] Farklı veri türleri için farklı TTL tanımlı mı?

Potansiyel Sorunlar:
- [ ] `@Transactional` + `@Cacheable` birlikte — rollback durumunda cache temizleniyor mu?
- [ ] Cache'e konan obje'ler `Serializable` mi?
- [ ] Lazy-loaded JPA field'ları cache'e girmeye çalışılıyor mu?

**Çıktı:** Tablo formatında bulgular + her biri için önerilen düzeltme
