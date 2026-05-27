---
description: "K8s deployment durumunu analiz eder, sorunları tanılar ve çözüm önerir"
argument-hint: "[odak: deployment|ingress|secret|hpa|all]"
allowed-tools: Read, Bash, Glob
---

Elly'nin K3s Kubernetes deployment'ını analiz et. Odak: `$ARGUMENTS` (belirtilmezse "all" kabul et).

Önce `k8s/` dizinindeki ilgili YAML dosyalarını oku.

**Kontrol Alanları:**

`deployment`:
- `k8s/2a-app-deployment.yaml` — image tag, resource limits/requests, liveness/readiness probe
- Replica sayısı uygun mu?
- Environment variable'lar ConfigMap/Secret'tan mı geliyor?

`ingress`:
- `k8s/4-ingress.yaml` — routing kuralları, TLS sertifika referansı
- `k8s/5-cluster-issuer.yaml` — Let's Encrypt config

`secret`:
- `k8s/1-secret.*.yaml` — Secret referansları deployment'ta doğru kullanılıyor mu?
- Hassas değerler base64 encoded mi?

`hpa`:
- `k8s/5-hpa.yaml` — min/max replica, CPU/memory threshold'ları
- `k8s/2b-app-burst.yaml` — burst politikası

`all`: Yukarıdakilerin tamamı

**Çıktı:**
- Tespit edilen sorunlar (varsa)
- Önerilen düzeltme komutları (`kubectl apply`, `kubectl rollout restart` vb.)
- Deployment sağlığını kontrol etmek için `kubectl` komutları

## kubectl Yararlı Pattern'ler

**Yerel SQL dosyasını pod içindeki psql'e gönder** (`-f local` ÇALIŞMAZ — `-f` pod içindeki dosya bekler):
```bash
# Pipe ile (en kısa yol — -i kullan, -it değil)
kubectl exec -i postgres-basedb-0  -n elly -- psql -U postgres -d elly_basedb  < db-migration-X.sql
kubectl exec -i postgres-tenant1-0 -n elly -- psql -U postgres -d elly_tenant1 < db-migration-X.sql
kubectl exec -i postgres-tenant2-0 -n elly -- psql -U postgres -d elly_tenant2 < db-migration-X.sql

# Veya kopyala-sonra-çalıştır
kubectl cp db-migration-X.sql elly/postgres-basedb-0:/tmp/m.sql
kubectl exec -it postgres-basedb-0 -n elly -- psql -U postgres -d elly_basedb -f /tmp/m.sql
```

**Pod yeniden başlatma (image değişmediyse):**
```bash
kubectl rollout restart deployment/elly-app -n elly
```

**Crash sebebini görme:**
```bash
kubectl logs -n elly <pod> --previous | tail -50   # önceki container log'u
kubectl describe pod -n elly <pod>                 # event'ler + probe failure detayı
```

**CrashLoopBackOff sık sebepler:**
- Migration eksik → Hibernate `validate` startup'ta basedb'de kolon bulamıyor
- startupProbe `timeoutSeconds: 1` → DB query 1sn'den uzun → probe fail (artık 5)
- RabbitMQ down → AMQP listener startup'ta exception
