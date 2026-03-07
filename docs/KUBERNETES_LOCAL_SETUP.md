# 🚀 GCP VM + K3s Deployment Rehberi

Bu rehber, Google Cloud'da tek bir VM oluşturup K3s ile Kubernetes çalıştırmayı anlatır.
Her adımda **neden** yapıldığı açıklanmıştır.

---

## 📖 Mimari

```
┌──────────────────────────────────────────────────────┐
│              GCP Compute Engine VM                   │
│              (Ubuntu 22.04, e2-standard-2)           │
│                                                      │
│   ┌─────────────────────────────────────────────┐    │
│   │              K3s Cluster                     │    │
│   │                                              │    │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │    │
│   │  │ App Pod 1│  │ App Pod 2│  │  Zipkin   │   │    │
│   │  └────┬─────┘  └────┬─────┘  └──────────┘   │    │
│   │       │              │                       │    │
│   │  ┌────┴─────┐  ┌────┴─────┐                  │    │
│   │  │Postgres-1│  │Postgres-2│                  │    │
│   │  └──────────┘  └──────────┘                  │    │
│   │                                              │    │
│   │  Traefik Ingress (K3s built-in)              │    │
│   └──────────────────────────────────────────────┘    │
│                     │                                 │
└─────────────────────┼─────────────────────────────────┘
                      │
               İnternet (:80, :443)
```

---

## ADIM 1: GCP'de VM Oluştur

### 1.1 — Google Cloud Console'dan

1. [Google Cloud Console](https://console.cloud.google.com/) → **Compute Engine** → **VM instances**
2. **Create Instance** tıkla
3. Ayarlar:

| Ayar | Değer | Neden? |
|------|-------|--------|
| **Name** | `elly-server` | İstediğin isim |
| **Region** | `europe-west1` (Belçika) | Türkiye'ye yakın, düşük latency |
| **Machine type** | `e2-standard-2` (2 vCPU, 8GB RAM) | Spring Boot + 2x Postgres + monitoring için yeterli |
| **Boot disk** | Ubuntu 22.04 LTS, 50GB SSD | K3s resmi olarak Ubuntu destekler |
| **Firewall** | ✅ Allow HTTP, ✅ Allow HTTPS | Dışarıdan erişim için |

### 1.2 — Veya gcloud CLI ile

```bash
gcloud compute instances create elly-server \
  --zone=europe-west1-b \
  --machine-type=e2-standard-2 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=50GB \
  --boot-disk-type=pd-ssd \
  --tags=http-server,https-server
```

> **Neden `e2-standard-2`?**
> - 2 vCPU + 8GB RAM → Spring Boot (1.5GB) + 2x Postgres (2x1GB) + Traefik + Monitoring
> - Aylık maliyet: ~$50
> - Daha ucuz istenirse `e2-medium` (2 vCPU, 4GB) da olabilir ama sıkışabilir

---

## ADIM 2: VM'e Bağlan ve Sistem Hazırla

```bash
# SSH ile bağlan
gcloud compute ssh elly-server --zone=europe-west1-b
```

```bash
# Sistem güncellemesi
sudo apt update && sudo apt upgrade -y

# Docker kur (image build için gerekli)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Oturumu yenile (docker grubunu aktifleştir)
newgrp docker
```

> **Neden Docker?**
> K3s container runtime olarak `containerd` kullanır (Docker'a gerek yok).
> Ama biz Docker'ı **image build etmek** için kullanıyoruz (`docker build`).

---

## ADIM 3: K3s Kur

```bash
# K3s'i kur (30 saniyede tamamlanır)
curl -sfL https://get.k3s.io | sh -
```

> **Bu komut ne yapar?**
> - Kubernetes API Server, etcd, scheduler, controller → hepsini tek binary olarak kurar
> - `kubectl` otomatik gelir
> - **Traefik** Ingress Controller otomatik kurulur
> - **local-path** Storage Class otomatik kurulur
> - `systemd` servisi olarak çalışır (VM restart olunca K3s de otomatik başlar)

```bash
# Çalıştığını doğrula
sudo kubectl get nodes

# Çıktı:
# NAME           STATUS   ROLES                  AGE   VERSION
# elly-server    Ready    control-plane,master   30s   v1.29.x+k3s1
```

```bash
# kubectl'i sudo olmadan kullanabilmek için
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
export KUBECONFIG=~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
```

> **Neden bu son adım?**
> K3s varsayılan olarak `/etc/rancher/k3s/k3s.yaml` dosyasını kullanır ve root gerektirir.
> Kopyalayarak normal kullanıcıyla da `kubectl` çalıştırabilirsin.

---

## ADIM 4: Proje Dosyalarını VM'e Aktar

```bash
# Seçenek 1: Git ile (önerilir)
cd ~
git clone https://github.com/your-repo/elly.git
cd elly

# Seçenek 2: gcloud scp ile (lokaldeki dosyaları kopyala)
# Mac'inden çalıştır:
# gcloud compute scp --recurse ./k8s elly-server:~/elly/k8s --zone=europe-west1-b
# gcloud compute scp ./Dockerfile elly-server:~/elly/ --zone=europe-west1-b
# gcloud compute scp -r ./src elly-server:~/elly/ --zone=europe-west1-b
# gcloud compute scp ./pom.xml elly-server:~/elly/ --zone=europe-west1-b
```

---

## ADIM 5: Docker Image Oluştur

```bash
cd ~/elly

# Image'ı build et
docker build -t elly-cms:latest .
```

> **Neden local build?**
> Registry (Docker Hub, GCR) kullanmadan, direkt VM üzerinde image oluşturuyoruz.
> K3s bu image'ı görebilmesi için import etmemiz gerekiyor:

```bash
# Image'ı K3s'in containerd'sine aktar
docker save elly-cms:latest | sudo k3s ctr images import -
```

> **Bu adım kritik!**
> K3s Docker yerine `containerd` kullanır. `docker build` ile oluşturduğumuz image
> containerd'de görünmez. `docker save | k3s ctr images import` pipe'ı ile aktarıyoruz.

---

## ADIM 6: Secret Değerlerini Hazırla

```bash
# Şifrelerini base64 ile encode et
echo -n 'guclu-db-sifresi-buraya' | base64
echo -n 'guclu-jwt-secret-min-32-karakter' | base64
```

`k8s/1-configmap-secret.yaml` dosyasındaki tüm `CHANGEME_BASE64_ENCODED` değerlerini
hesapladığın base64 değerleriyle değiştir:

```bash
# Dosyayı düzenle
nano ~/elly/k8s/1-configmap-secret.yaml
```

> **Güvenlik notu:** Production'da bu dosyayı Git'e **ASLA** commit etme!
> `.gitignore`'a ekle veya Sealed Secrets / Vault kullan.

---

## ADIM 7: Metrics Server Kur (HPA için)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

> **Neden?**
> HPA (otomatik ölçekleme) pod'ların CPU/Memory metriklerine bakarak karar verir.
> Bu metrikleri toplayan Metrics Server, K3s'te varsayılan olarak **gelmez**.

---

## ADIM 8: K8s Manifest'lerini Uygula

```bash
cd ~/elly

# Sırayla uygula
kubectl apply -f k8s/1-configmap-secret.yaml
kubectl apply -f k8s/2-deployment.yaml
kubectl apply -f k8s/3-service.yaml
kubectl apply -f k8s/4-ingress.yaml
kubectl apply -f k8s/5-hpa.yaml
kubectl apply -f k8s/6-monitoring.yaml
```

---

## ADIM 9: Durumu İzle

```bash
# Pod'ları izle (Running olana kadar bekle)
kubectl get pods -n elly -w

# Beklenen çıktı (2-3 dakika):
# NAME                        READY   STATUS    AGE
# elly-app-xxxxx-yyyyy        1/1     Running   2m
# elly-app-xxxxx-zzzzz        1/1     Running   2m
# postgres-tenant1-0          1/1     Running   3m
# postgres-tenant2-0          1/1     Running   3m
# zipkin-xxxxx-yyyyy          1/1     Running   2m

# Sorun varsa detayları gör
kubectl describe pod <pod-adı> -n elly
kubectl logs <pod-adı> -n elly

# HPA durumu
kubectl get hpa -n elly
```

---

## ADIM 10: DNS ve Erişim Ayarla

### 10.1 — GCP Firewall (zaten açık olmalı)
```bash
# HTTP/HTTPS portlarını aç (oluşturma sırasında açtıysanız gerek yok)
gcloud compute firewall-rules create allow-http-https \
  --allow=tcp:80,tcp:443 \
  --target-tags=http-server,https-server
```

### 10.2 — Static IP al
```bash
# VM'in dış IP'sini sabitle (VM restart'ta değişmesin)
gcloud compute addresses create elly-static-ip \
  --addresses=$(gcloud compute instances describe elly-server \
    --zone=europe-west1-b --format='get(networkInterfaces[0].accessConfigs[0].natIP)') \
  --region=europe-west1
```

### 10.3 — DNS Ayarla
Domain sağlayıcında (Cloudflare, Google Domains vb.) **A record** ekle:

```
Tip: A
Ad:  your-domain.com
IP:  <VM'in static IP'si>
TTL: Auto
```

### 10.4 — Ingress Host'unu Güncelle
```bash
# k8s/4-ingress.yaml dosyasında "your-domain.com" → gerçek domain'inle değiştir
nano ~/elly/k8s/4-ingress.yaml

# Değişikliği uygula
kubectl apply -f k8s/4-ingress.yaml
```

### Test:
```bash
# VM üzerinden test
curl http://localhost/actuator/health

# Dışarıdan test (domain'in verildikten sonra)
curl http://your-domain.com/actuator/health
```

---

## ADIM 11: SSL Sertifikası (HTTPS)

```bash
# cert-manager kur
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml

# Let's Encrypt ClusterIssuer oluştur
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: traefik
EOF
```

Sonra `k8s/4-ingress.yaml`'da bu satırı aç (yorumdan çıkar):
```yaml
cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

> **Neden cert-manager?**
> Let's Encrypt'ten **ücretsiz** SSL sertifikası alır ve otomatik yeniler.

---

## ADIM 12: Monitoring (Prometheus + Grafana)

```bash
# Helm kur
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Prometheus + Grafana kur
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --set grafana.adminPassword=your-grafana-password

# Grafana'ya erişim (SSH tunnel ile)
# Mac'inden çalıştır:
# gcloud compute ssh elly-server --zone=europe-west1-b -- -L 3000:localhost:3000
# kubectl port-forward svc/monitoring-grafana 3000:80 -n monitoring
# Tarayıcı: http://localhost:3000 (admin / your-grafana-password)
```

---

## 🔄 Uygulama Güncelleme Akışı

Kod değişikliği yaptığında:

```bash
# 1. Yeni image build et
cd ~/elly
git pull
docker build -t elly-cms:latest .

# 2. K3s'e import et
docker save elly-cms:latest | sudo k3s ctr images import -

# 3. Pod'ları yeniden başlat (yeni image'ı alsınlar)
kubectl rollout restart deployment/elly-app -n elly

# 4. Durumu izle
kubectl rollout status deployment/elly-app -n elly
```

---

## 🧹 Temizlik (Gerekirse)

```bash
# Tüm K8s kaynaklarını sil
kubectl delete -f k8s/ --ignore-not-found
kubectl delete namespace elly

# K3s'i tamamen kaldır
/usr/local/bin/k3s-uninstall.sh
```

---

## 🔍 Sık Kullanılan Komutlar

| Komut | Ne yapar? |
|-------|-----------|
| `kubectl get pods -n elly` | Pod listesi |
| `kubectl logs -f <pod> -n elly` | Canlı loglar |
| `kubectl describe pod <pod> -n elly` | Pod detayı (hata ayıklama) |
| `kubectl exec -it <pod> -n elly -- /bin/sh` | Pod'a bağlan (shell) |
| `kubectl top pods -n elly` | CPU/Memory kullanımı |
| `kubectl get events -n elly --sort-by=.lastTimestamp` | Son olaylar |
| `kubectl rollout restart deployment/elly-app -n elly` | Pod'ları yeniden başlat |
| `kubectl scale deployment/elly-app --replicas=3 -n elly` | Manuel ölçekle |
| `sudo systemctl status k3s` | K3s servis durumu |
| `sudo journalctl -u k3s -f` | K3s logları |
