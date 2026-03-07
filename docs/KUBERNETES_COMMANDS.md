# 🚀 Kubernetes Komut Rehberi (K3s / k3d)

Tüm işlemler için hızlı referans. Sıralı olarak çalıştırılmalıdır.

> **Lokal:** k3d (K3s Docker içinde) • **Sunucu:** K3s (direkt OS üzerinde)
> Aynı K3s, aynı komutlar, aynı YAML'lar.

---

## 1. Cluster Başlatma

### Lokal (k3d)
```bash
# İlk kez cluster oluştur
k3d cluster create elly --servers 1 --agents 0 \
  -p "8080:80@loadbalancer" \
  -p "8443:443@loadbalancer"

# Durdurulmuş cluster'ı başlat
k3d cluster start elly

# Cluster durdur
k3d cluster stop elly
```

### Sunucu (K3s)
```bash
# K3s kur (tek komut, otomatik başlar)
curl -sfL https://get.k3s.io | sh -

# K3s otomatik başlar, systemd servisi olarak çalışır
# Restart sonrası otomatik ayağa kalkar
```

---

## 2. Docker Image Build ve Yükleme

### Lokal (k3d)
```bash
# Image build et
docker build -t elly-cms:latest .

# k3d cluster'a import et
k3d image import elly-cms:latest -c elly
```

### Sunucu (K3s)
```bash
# Image build et
docker build -t elly-cms:latest .

# K3s'e import et
docker save elly-cms:latest | sudo k3s ctr images import -
```

---

## 3. Tüm Servisleri Ayağa Kaldırma (Sıralı)

```bash
# Namespace + Config + Secret
kubectl apply -f k8s/0-namespace.yaml
kubectl apply -f k8s/1-configmap-secret.yaml

# PostgreSQL veritabanları
kubectl apply -f k8s/2c-postgres.yaml

# Service'ler (pod'lar arası iletişim)
kubectl apply -f k8s/3-service.yaml

# Spring Boot app (2 base pod)
kubectl apply -f k8s/2a-app-deployment.yaml

# Burst pod'lar (yoğun trafik için)
kubectl apply -f k8s/2b-app-burst.yaml

# Ingress (dış erişim)
kubectl apply -f k8s/4-ingress.yaml

# HPA (otomatik ölçekleme)
kubectl apply -f k8s/5-hpa.yaml

# Backup CronJob (gece 03:00 otomatik)
kubectl apply -f k8s/7-backup-cronjob.yaml
```

**Veya tek seferde:**
```bash
kubectl apply -f k8s/
```

---

## 4. Pod Durumu Kontrol

```bash
# Tüm pod'ları gör
kubectl get pods -n elly

# Tüm kaynakları gör
kubectl get all -n elly

# Pod detayı (hata ayıklama)
kubectl describe pod <pod-adı> -n elly

# Canlı loglar
kubectl logs -f <pod-adı> -n elly

# CPU/Memory kullanımı
kubectl top pods -n elly
```

---

## 5. App Erişimi

### Lokal (port-forward gerekli)
```bash
kubectl port-forward svc/elly-app-service 8080:8080 -n elly &
```

### Sunucu (Ingress otomatik yönlendirir, port-forward gereksiz)

| Adres | Ne? |
|-------|-----|
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/swagger-ui.html | Swagger API docs |
| http://localhost:8080/api/... | API endpoint'leri |

---

## 6. DB Portlarını Dışarı Açma (Sadece Lokal)

```bash
# Tenant 1 → localhost:5433
kubectl port-forward svc/postgres-tenant1 5433:5432 -n elly &

# Tenant 2 → localhost:5434
kubectl port-forward svc/postgres-tenant2 5434:5432 -n elly &
```

| DB | Host | Port | User | Password | Database |
|----|------|------|------|----------|----------|
| Tenant 1 | 127.0.0.1 | 5433 | postgres | 123456 | elly_tenant1 |
| Tenant 2 | 127.0.0.1 | 5434 | postgres | 123456 | elly_tenant2 |

```bash
# Veya direkt pod içinden SQL çalıştır
kubectl exec -it postgres-tenant1-0 -n elly -- psql -U postgres -d elly_tenant1
kubectl exec -it postgres-tenant2-0 -n elly -- psql -U postgres -d elly_tenant2
```

---

## 7. Monitoring Erişimi (Sadece Lokal)

```bash
# Grafana → localhost:3000
kubectl port-forward svc/grafana 3000:3000 -n elly &

# Prometheus → localhost:9090
kubectl port-forward svc/prometheus 9090:9090 -n elly &

# Zipkin → localhost:9411
kubectl port-forward svc/zipkin 9411:9411 -n elly &
```

| Araç | Adres | Kullanıcı | Şifre |
|------|-------|-----------|-------|
| Grafana | http://localhost:3000 | admin | admin123 |
| Prometheus | http://localhost:9090 | - | - |
| Zipkin | http://localhost:9411 | - | - |

---

## 7. Uygulama Güncelleme (Yeni Kod Deploy)

### Lokal (k3d)
```bash
docker build -t elly-cms:latest .
k3d image import elly-cms:latest -c elly
kubectl rollout restart deployment/elly-app -n elly
kubectl rollout status deployment/elly-app -n elly
```

### Sunucu (K3s)
```bash
docker build -t elly-cms:latest .
docker save elly-cms:latest | sudo k3s ctr images import -
kubectl rollout restart deployment/elly-app -n elly
kubectl rollout status deployment/elly-app -n elly
```

---

## 8. Ölçekleme

```bash
# Manuel olarak pod sayısını değiştir
kubectl scale deployment/elly-app --replicas=3 -n elly

# HPA durumu (otomatik ölçekleme)
kubectl get hpa -n elly
```

---

## 9. Backup

```bash
# Manuel backup al (anında)
kubectl create job --from=cronjob/postgres-backup manual-backup -n elly

# Backup loglarını gör
kubectl logs job/manual-backup -n elly

# Backup dosyasını indir
kubectl cp elly/<backup-pod>:/backups/tenant1-2026-03-05_0300.sql.gz ./backup.sql.gz

# Backup'tan geri yükle
gunzip backup.sql.gz
kubectl exec -i postgres-tenant1-0 -n elly -- psql -U postgres -d elly_tenant1 < backup.sql
```

---

## 10. Durdurma ve Temizlik

### Lokal (k3d)
```bash
# Cluster durdur (state korunur, RAM serbest kalır)
k3d cluster stop elly

# Cluster başlat (pod'lar otomatik geri gelir)
k3d cluster start elly

# Cluster tamamen sil (her şey silinir!)
k3d cluster delete elly
```

### Sunucu (K3s)
```bash
# K3s durdur
sudo systemctl stop k3s

# K3s başlat
sudo systemctl start k3s

# K3s tamamen kaldır
/usr/local/bin/k3s-uninstall.sh
```

---

## 11. Sık Kullanılan Kısayollar

| Komut | Ne yapar? |
|-------|-----------|
| `kubectl get all -n elly` | Tüm kaynakları listele |
| `kubectl get events -n elly --sort-by=.lastTimestamp` | Son olaylar |
| `kubectl exec -it <pod> -n elly -- /bin/sh` | Pod'a shell bağlan |
| `k3d cluster list` | Cluster listesi (lokal) |
| `kubectl config current-context` | Hangi cluster'a bağlısın? |
