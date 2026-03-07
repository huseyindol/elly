#!/bin/bash
# ============================================
# Elly CMS — GCP VM İlk Kurulum Script'i
# ============================================
# Bu script GCP VM'de k3s ve gerekli bileşenleri kurar.
# Kullanım: chmod +x server-setup.sh && ./server-setup.sh
#
# Gereksinimler:
#   - Ubuntu 24.04 LTS
#   - Root veya sudo yetkisi
#   - İnternet bağlantısı
set -euo pipefail

echo "============================================"
echo "🚀 Elly CMS — Sunucu Kurulumu"
echo "============================================"

# ==========================================
# 1. Sistem Güncelleme
# ==========================================
echo ""
echo "📦 1/5 — Sistem güncelleniyor..."
sudo apt update && sudo apt upgrade -y

# ==========================================
# 2. K3s Kurulumu
# ==========================================
echo ""
echo "☸️  2/5 — K3s kuruluyor..."
if command -v k3s &> /dev/null; then
    echo "  ➜ K3s zaten kurulu: $(k3s --version)"
else
    curl -sfL https://get.k3s.io | sh -
    echo "  ✅ K3s kuruldu: $(k3s --version)"
fi

# kubectl alias ve kubeconfig ayarla
echo ""
echo "🔧 kubectl ayarlanıyor..."
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
export KUBECONFIG=~/.kube/config

# .bashrc'ye ekle
if ! grep -q "KUBECONFIG" ~/.bashrc; then
    echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
fi

# k3s'in hazır olmasını bekle
echo "⏳ K3s'in hazır olması bekleniyor..."
sudo kubectl wait --for=condition=Ready nodes --all --timeout=60s
echo "  ✅ K3s hazır!"

# ==========================================
# 3. Namespace Oluştur
# ==========================================
echo ""
echo "📁 3/5 — Namespace oluşturuluyor..."
sudo kubectl create namespace elly --dry-run=client -o yaml | sudo kubectl apply -f -
echo "  ✅ 'elly' namespace hazır."

# ==========================================
# 4. GHCR Pull Secret (Private Repo İçin)
# ==========================================
echo ""
echo "🔑 4/5 — GHCR Pull Secret ayarlanıyor..."
echo ""
echo "  ⚠️  Private repo kullanıyorsanız, aşağıdaki komutu çalıştırın:"
echo "  (Public repo ise bu adımı atlayabilirsiniz)"
echo ""
echo "  sudo kubectl create secret docker-registry ghcr-pull-secret \\"
echo "    --docker-server=ghcr.io \\"
echo "    --docker-username=GITHUB_USERNAME \\"
echo "    --docker-password=GITHUB_PAT \\"
echo "    -n elly"
echo ""
echo "  GitHub PAT oluşturmak için:"
echo "  GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)"
echo "  Scope: read:packages"

# ==========================================
# 5. k8s Manifest'leri Uygula (İlk Kurulum)
# ==========================================
echo ""
echo "📋 5/5 — k8s manifest'leri uygulanıyor..."
echo ""
echo "  ⚠️  İlk kurulum için tüm manifest'leri uygulamanız gerekir."
echo "  Aşağıdaki komutları sırasıyla çalıştırın:"
echo ""
echo "  # 1. Önce dosyaları sunucuya kopyala (lokal makineden):"
echo "  scp -r k8s/ USER@SERVER_IP:/tmp/k8s/"
echo ""
echo "  # 2. Sunucuda uygula:"
echo "  sudo kubectl apply -f /tmp/k8s/0-namespace.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/1-configmap.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/1-secret.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/2c-postgres.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/3-service.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/2a-app-deployment.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/2b-app-burst.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/4-ingress.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/5-hpa.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/6-monitoring.yaml"
echo "  sudo kubectl apply -f /tmp/k8s/7-backup-cronjob.yaml"

echo ""
echo "============================================"
echo "✅ Sunucu kurulumu tamamlandı!"
echo "============================================"
echo ""
echo "📌 Sonraki adımlar:"
echo "  1. GitHub repo Settings → Secrets'a gerekli secret'ları ekleyin"
echo "  2. main branch'e push yapın"
echo "  3. GitHub Actions'tan deploy durumunu takip edin"
echo ""
