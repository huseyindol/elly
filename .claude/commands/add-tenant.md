---
description: "Yeni bir tenant eklemek için gereken tüm değişiklikleri listeler ve hazırlar"
argument-hint: "<tenant-adı>"
allowed-tools: Read, Write, Edit, Glob, Grep
---

`$ARGUMENTS` adında yeni bir tenant eklemek için gereken tüm değişiklikleri hazırla.

Önce şu dosyaları oku ve mevcut tenant yapısını anla:
- `src/main/resources/application.properties` veya `application-docker.properties`
- `src/main/java/com/cms/config/DataSourceConfig.java`
- `k8s/1-configmap.yaml`
- `k8s/1-secret.template.yaml` (varsa)
- `.github/workflows/deploy.yml`

**Yapılacaklar (sırayla):**

1. **application.properties** — yeni tenant DB config'i ekle:
   ```
   spring.datasource.<tenant-adı>.url=...
   spring.datasource.<tenant-adı>.username=...
   spring.datasource.<tenant-adı>.password=...
   ```

2. **DataSourceConfig.java** — tenant map'e ekle (mevcut pattern'i takip et)

3. **k8s/1-configmap.yaml** — yeni env değişkenlerini ekle

4. **Veritabanı oluşturma SQL'i** yaz:
   ```sql
   CREATE DATABASE elly_<tenant-adı>;
   CREATE USER elly_<tenant-adı>_user WITH PASSWORD '...';
   GRANT ALL PRIVILEGES ON DATABASE elly_<tenant-adı> TO elly_<tenant-adı>_user;
   ```

5. **GitHub Actions secrets** listesi — eklenecek secret isimleri

Her adım için tam değişiklik içeriğini göster (diff formatında).
