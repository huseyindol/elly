#!/bin/bash

echo "=========================================="
echo "🔍 Actuator Metrikleri Kontrol Aracı"
echo "=========================================="
echo ""

# Health check
echo "1️⃣ Health Check:"
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
echo ""

# Tüm metrikleri listele
echo "2️⃣ Mevcut Metrikler:"
curl -s http://localhost:8080/actuator/metrics | python3 -m json.tool | head -20
echo ""

# HTTP request metrikleri
echo "3️⃣ HTTP Request Metrikleri (Genel):"
curl -s http://localhost:8080/actuator/metrics/http.server.requests | python3 -m json.tool | head -30
echo ""

# Mevcut URI'leri listele
echo "4️⃣ Mevcut API Endpoint'leri:"
curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | \
  python3 -m json.tool | \
  grep -A 30 '"uri"' | \
  grep -E '"/api/' | \
  sed 's/.*"\(.*\)".*/\1/' | \
  sort -u | \
  head -10
echo ""

# Connection pool
echo "5️⃣ HikariCP Connection Pool:"
echo "  Aktif: $(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active 2>/dev/null | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))' 2>/dev/null || echo 'N/A')"
echo "  Max: $(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.max 2>/dev/null | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))' 2>/dev/null || echo 'N/A')"
echo ""

# JVM Memory
echo "6️⃣ JVM Memory:"
echo "  Kullanılan: $(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used 2>/dev/null | python3 -c 'import sys, json; val=json.load(sys.stdin)["measurements"][0]["value"]; print(f"{val/1024/1024:.2f} MB")' 2>/dev/null || echo 'N/A')"
echo ""

echo "=========================================="
echo "✅ Kontrol tamamlandı!"
echo "=========================================="
