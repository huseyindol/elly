---
description: "Belirtilen dosya veya paketi güvenlik açıklarına karşı inceler"
argument-hint: "[dosya-yolu veya paket-adı, boş bırakılırsa tüm proje]"
allowed-tools: Read, Grep, Glob
---

`$ARGUMENTS` için kapsamlı güvenlik incelemesi yap. Elly'nin özel bağlamını göz önünde bulundur.

**Kontrol Listesi:**

JWT & Auth:
- [ ] JWT claim manipülasyonu riski (`tenantId`, `loginSource` claim'leri imzalı mı?)
- [ ] Refresh token güvenliği ve rotation
- [ ] Token expiry süreleri uygun mu?

Multi-Tenant İzolasyonu:
- [ ] Farklı tenant'ların birbirinin datasına erişimi mümkün mü?
- [ ] Redis cache key'lerinde tenantId var mı?
- [ ] Query'lerde tenant filtresi eksik mi?

OAuth2:
- [ ] `redirect_uri` whitelist kontrolü
- [ ] `state` parametresi CSRF koruması

Spring Security:
- [ ] `.permitAll()` olan endpoint'ler gerçekten public olmalı mı?
- [ ] CORS konfigürasyonu çok geniş mi?
- [ ] CSRF koruması devre dışı mı ve neden?

Veri Güvenliği:
- [ ] Native query / JPQL'de SQL injection riski
- [ ] Log'larda hassas veri (şifre, token, kişisel bilgi)?
- [ ] Response'larda gereksiz entity field'ları expose ediliyor mu?

**Rapor Formatı:**

🔴 KRİTİK | 🟡 ORTA | 🟢 DÜŞÜK

Her bulgu için: `dosya:satır` — sorun açıklaması — önerilen düzeltme
