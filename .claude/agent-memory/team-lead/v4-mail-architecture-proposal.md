# v4 Mail Mimarisi — Template Hosting Kararı

> Kullanıcı isteği: "elly-admin-panel projesinde template hazır edip buradan POST ile bilgileri elly-admin-panel'e gönderelim response'ta html döneyim buradan mail gönderimini yapalım." CMS'in yük almasını istemiyor — şu an sadece API barındırıyor.

---

## Tarihçe

- **v1/v2:** Thymeleaf template'leri CMS içinde, `src/main/resources/templates/emails/*.html` altında. 3 template var: `welcome`, `notification`, `form-notification`.
- **Sorun:** Template'i değiştirmek için kod deploy gerekiyor. İçerik yönetimi geliştirici işi, operasyon/pazarlama ekibi kendi başına düzenleyemiyor.
- **v3:** Retry endpoint eklendi (bu iterasyon). Template işine dokunulmadı.

---

## Tasarım Seçenekleri

Kullanıcının önerisi (A) yanında iki alternatifi de masaya yatırıyoruz çünkü "her mail'de network hop" ve "tek arıza noktası" problemleri çözülebilir.

### Opsiyon A — Panel On-Demand Render (kullanıcının önerisi)

```
elly CMS ─────────► elly-admin-panel ─────► (Thymeleaf/handlebars) ─────► HTML
    ▲                   │
    └─── SMTP ◄─── HTML response
```

**Akış:**
1. `EmailQueueService` consumer (RabbitMQ'da) mail işlerken `POST https://panel.../api/render` yapar.
2. Panel (Next.js / Nuxt) template'i render eder, HTML döner.
3. CMS HTML'i `JavaMailSender` ile gönderir.

**Artı:**
- Template'ler panel repo'sunda yaşar; tek kaynak.
- CMS içinde template dosyası/servis yok — kullanıcının istediği hafiflik.
- Panel ekibi template'i değiştirir, CMS deploy olmaz.

**Eksi:**
- **Her mailde network hop.** p99 latency artar, retry senaryoları karmaşıklaşır (hem SMTP hem panel tarafında hata olabilir).
- **Tek arıza noktası.** Panel down olursa CMS mail gönderemez. Consumer queue birikir.
- **Sıkı coupling.** CMS service-to-service auth (mTLS/JWT-signed) + rate limit + timeout çemberi kurmak zorunda. Bu tam da kullanıcının "CMS hafif kalsın" hedefine ters.
- Panel Next.js/SSR ise render maliyeti (cold start, memory) CMS'i değil panel'i ağırlaştırır — kullanıcı yükü bir yerden bir yere taşımış olur.

### Opsiyon B — Panel Push to CDN / Git (hafif sync)

```
elly-admin-panel ──► PUT s3://templates/welcome.html (or git push)
                                   │
                                   ▼
                     elly CMS ──► önyüklemede + TTL cache ile pull
                                   │
                                   ▼
                      Thymeleaf local render ──► SMTP
```

**Akış:**
1. Panel UI'da template düzenleyen kullanıcı kaydet der → panel S3'e (veya git repo'ya) push eder.
2. CMS S3'ü 5 dk cache ile okur (veya git webhook ile invalidate).
3. Mail gönderirken CMS yerelinden okur, Thymeleaf ile render eder.

**Artı:**
- Runtime'da cross-service call **yok**.
- CMS down scenario'su yok.
- CDN edge cache'lenebilir.

**Eksi:**
- Yeni altyapı (S3 bucket + IAM + sync job) gerekir.
- Template değişikliği ~5 dk gecikir (cache TTL).
- Preview/staging template'leri için ek logic.

### Opsiyon C — Template Registry CMS DB'de, Panel CRUD (**ÖNERİ**)

```
elly-admin-panel ──HTTP──► POST /api/v1/email-templates (CMS)
                                    │
                                    ▼
                         PostgreSQL email_templates tablosu
                                    │
                                    ▼ (Redis cache, TTL 10dk)
                         Thymeleaf engine ──► render ──► SMTP
```

**Akış:**
1. Panel UI'da admin `welcome` template'ini düzenler → `PUT /api/v1/email-templates/welcome` çağırır.
2. CMS DB'ye yazar, `@CacheEvict("emailTemplates")` ile cache temizler.
3. `EmailQueueService` consumer render anında `templateRegistry.load("welcome")` ile DB'den çeker (Redis cache), Thymeleaf ile render eder.
4. Classpath fallback: DB'de yoksa `templates/emails/{name}.html` kullanılır (eski davranış).

**Artı:**
- CMS runtime cross-service call yapmaz → v2 performans profiline aynı kalır.
- Panel sadece CRUD UI sağlar; render mantığı CMS'in sorumluluğunda (zaten Thymeleaf entegre).
- Redis cache ile DB round-trip sadece cache miss'de.
- Multi-tenant template destek — her tenant kendi override'ını tutabilir (tenant_id kolon).
- Classpath fallback ile **zero-downtime migration**: mevcut 3 template çalışmaya devam eder, DB'de override edilene kadar.
- Template versiyonlama (`email_template_revisions` tablosu) kolay eklenir.
- Panel "bu template'i kullananlar" listesi için CMS'ten `GET /email-templates/{key}/usage` endpoint'i sorabilir.

**Eksi:**
- Template içeriği CMS DB'sinde yaşar (ama bu sadece text, CMS iş mantığı değil — CMS'e yük binmez).
- Panel template düzenleme UI'sini yazmak zorunda (ama bu her opsiyon için gerekli).

---

## Karar: **Opsiyon C (Template Registry)**

Neden A değil: Her mailde network hop + tek arıza noktası + cross-service auth ekleme — kullanıcının hafiflik hedefine ters. Yükü CMS'ten panel'e taşımak yük azaltmaz, sadece yer değiştirir.

Neden B değil: Yeni altyapı (S3/IAM) gerekiyor, gelecekte template versiyonlama/audit için ekstra iş. CMS'imizde zaten PostgreSQL + Redis var, onu kullanalım.

---

## Uygulama Planı (v4 Iteration)

### Backend (elly CMS)

**1. Yeni Entity: `EmailTemplate`**

```java
@Entity
@Table(name = "email_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "template_key"}))
public class EmailTemplate extends BaseEntity {
    @Column(name = "tenant_id", nullable = true)  // null = global (tüm tenantlar için)
    private String tenantId;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;    // "welcome", "form-notification", vs.

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;        // Thymeleaf expression: "Hoşgeldin [[${userName}]]"

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;       // Tam Thymeleaf template

    @Column(name = "description", length = 500)
    private String description;    // Admin UI için

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "version", nullable = false)
    private Integer version;       // Optimistic lock / audit

    @Version
    private Long optimisticLockVersion;
}
```

**2. Yeni Servisler**

- `IEmailTemplateService` — CRUD + `loadByKey(key)` (tenant-aware, cacheable)
- `EmailTemplateRenderer` — Thymeleaf wrapper, önce DB'den çeker, yoksa classpath fallback
- Mevcut `EmailQueueService` değişir: `templateEngine.process(...)` yerine `templateRenderer.render(key, model)`

**3. Yeni Endpoint'ler**

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET | `/api/v1/email-templates` | `email_templates:read` | Liste (paginated, tenant filtered) |
| GET | `/api/v1/email-templates/{key}` | `email_templates:read` | Detay |
| POST | `/api/v1/email-templates` | `email_templates:manage` | Oluştur |
| PUT | `/api/v1/email-templates/{key}` | `email_templates:manage` | Güncelle |
| DELETE | `/api/v1/email-templates/{key}` | `email_templates:manage` | Sil (soft delete) |
| POST | `/api/v1/email-templates/{key}/preview` | `email_templates:read` | Dummy data ile render preview döndür |

**4. Permission'lar**

`PermissionConstants.java`'ya ekle:
```java
public static final String EMAIL_TEMPLATES_READ = "email_templates:read";
public static final String EMAIL_TEMPLATES_MANAGE = "email_templates:manage";
```

**5. Cache Stratejisi**

- `@Cacheable(value = "emailTemplates", key = "#tenantId + '::' + #key")` — TTL 10dk.
- Tenant-aware prefix'i Elly'nin mevcut Redis config'i otomatik halleder.
- Güncelleme/silme: `@CacheEvict(value = "emailTemplates", allEntries = true)`.

**6. Migration**

```sql
CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64),
    template_key VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    html_body TEXT NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    optimistic_lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_templates_tenant_key UNIQUE (tenant_id, template_key)
);

CREATE INDEX idx_email_templates_key ON email_templates(template_key);
CREATE INDEX idx_email_templates_active ON email_templates(active);
```

**7. Bootstrap / Seed**

İlk deploy'da classpath'taki 3 template'i global (tenant_id=NULL) olarak insert et. Script:

```java
@Component
@RequiredArgsConstructor
public class EmailTemplateBootstrapRunner implements ApplicationRunner {
    private final IEmailTemplateService service;

    @Override
    public void run(ApplicationArguments args) {
        seed("welcome", "Hoşgeldin", classpathLoad("welcome.html"));
        seed("notification", "Bildirim", classpathLoad("notification.html"));
        seed("form-notification", "Yeni Form Gönderimi", classpathLoad("form-notification.html"));
    }

    private void seed(String key, String subject, String body) {
        if (!service.existsByKey(null, key)) {
            service.createGlobal(key, subject, body, "Classpath'ten seed edildi.");
        }
    }
}
```

### Frontend (elly-admin-panel)

Bu repo ayrı, burada sadece nasıl entegre edileceği dökümanlanıyor.

**Sayfa: `/admin/email-templates`**

- Liste tablosu (key, subject, updated_at, active)
- "Yeni Template" butonu → form (key, subject, body Monaco editor ile HTML + Thymeleaf syntax highlight, description)
- Satır tıklanınca detay/edit sayfası
- "Preview" butonu → modal:
  - Sol: dummy JSON input (örnek: `{"userName": "Ahmet", "link": "https://..."}`)
  - Sağ: rendered HTML iframe
- "Save" → optimistic lock versiyonu ile PUT
- "Delete" → confirm modal + soft delete

**API Client (panel tarafında)**

```typescript
// panel/src/api/email-templates.ts
export const emailTemplatesApi = {
  list: (params) => http.get('/api/v1/email-templates', { params }),
  get: (key) => http.get(`/api/v1/email-templates/${key}`),
  create: (dto) => http.post('/api/v1/email-templates', dto),
  update: (key, dto) => http.put(`/api/v1/email-templates/${key}`, dto),
  remove: (key) => http.delete(`/api/v1/email-templates/${key}`),
  preview: (key, data) => http.post(`/api/v1/email-templates/${key}/preview`, { data }),
};
```

Authorization header'a admin JWT token eklenir (mevcut auth flow zaten panel'de var).

### Dependencies (CMS)

Zaten var: Thymeleaf, Spring Data JPA, Spring Cache (Redis).
**Yeni:** `org.jsoup:jsoup` (opsiyonel — template editor preview sanitization için).

### Test Stratejisi

- Unit: `EmailTemplateRendererTest` — DB hit, cache hit, classpath fallback, bozuk Thymeleaf syntax hata yakalama.
- Integration: Mevcut `EmailQueueServiceTest`'e yeni scenario ekle: DB-based template + fallback.
- Manuel: Panel'den edit → preview → form submit → gelen mail içeriği panel'deki content ile eşleşmeli.

---

## Göç Yolu (Zero-downtime)

**v3 → v4 migrasyonu için sıra:**

1. DB migration'ı uygula (yeni tablo ekle, mevcut flow'a dokunma).
2. Backend deploy: yeni entity/servis/endpoint'ler ekli, `EmailQueueService` classpath-first → DB-second okur (ilk iterasyonda backward compatible).
3. Bootstrap runner çalışır, 3 template global olarak seed edilir.
4. Panel deploy: template yönetim UI canlı.
5. Admin template'leri DB'de düzenler. CMS DB-first'e geçmek için config flag: `app.mail.template-source=db` (default) / `classpath`.
6. Sonraki release'de classpath template'ler silinir (opsiyonel).

**Rollback:** `app.mail.template-source=classpath` yaparak eski davranışa döner.

---

## Açık Sorular / v5'e Ertelenenler

- **Template versiyonlama / audit log** — Şu an sadece `optimistic_lock_version` var. Kim ne zaman değiştirdi tablosu ayrı iterasyon.
- **A/B testing** — Aynı `key` için iki variant + weighted routing. v5.
- **Template import/export** — ZIP olarak dışa aktarma (staging → prod migration). v5.
- **Rich editor (WYSIWYG)** — Şu an Monaco/HTML source düşünülüyor, ileride GrapeJS tarzı visual editor.
- **Layout/partial support** — Thymeleaf fragment desteği (header/footer'ı ayrı template'te tut). Backend hazır, sadece bootstrap + UI'a eklenecek.

---

## Özet

Kullanıcının "CMS hafif kalsın" hedefi **Opsiyon C ile tam karşılanır:**
- CMS runtime'da dış servise çağrı yapmaz (latency + tek arıza noktası yok).
- Template yönetimi panel'de (kullanıcı deneyimi istenilen yerde).
- CMS'e eklenen yük: 1 tablo + 1 servis + 1 renderer wrapper. Bu zaten mevcut altyapı ile (JPA + Redis + Thymeleaf) komposite edilir, yeni teknoloji katmanı **yok**.

Opsiyon A'nın tek avantajı "template dosyası CMS repo'sunda olmaması" idi — C de bunu DB ile sağlıyor, üstüne runtime coupling olmadan.

**Karar:** v4'e Opsiyon C ile başla. Bootstrap runner ile geri uyumluluk sağla, gerekirse classpath fallback kalmaya devam etsin.
