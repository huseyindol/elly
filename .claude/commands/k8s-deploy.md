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
