#!/bin/bash

# Kullanım: 
#   ./check-endpoint.sh "/api/v1/pages/{slug}"
#   ./check-endpoint.sh "/api/v1/pages/{slug}" "your-jwt-token"
#   ./check-endpoint.sh "/api/v1/pages/home" "your-jwt-token"

if [ -z "$1" ]; then
  echo "❌ Kullanım: ./check-endpoint.sh <endpoint-uri> [token]"
  echo "   Örnek: ./check-endpoint.sh \"/api/v1/pages/{slug}\""
  echo "   Örnek: ./check-endpoint.sh \"/api/v1/pages/home\" \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
  exit 1
fi

ENDPOINT=$1
TOKEN=$2

# Eğer gerçek bir endpoint path verilmişse (örn: /api/v1/pages/home)
# önce o endpoint'e istek atarak metrik oluştur
if [[ "$ENDPOINT" == *"/"* ]] && [[ "$ENDPOINT" != *"{"* ]]; then
  echo "🔄 Endpoint'e istek atılıyor (metrik oluşturmak için)..."
  
  # Token varsa Authorization header ekle
  if [ ! -z "$TOKEN" ]; then
    curl -s -o /dev/null -w "Status: %{http_code}\n" \
      -H "Authorization: Bearer $TOKEN" \
      "http://localhost:8080$ENDPOINT"
  else
    curl -s -o /dev/null -w "Status: %{http_code}\n" \
      "http://localhost:8080$ENDPOINT"
  fi
  
  echo ""
  sleep 1
  
  # Gerçek path'i Spring Boot formatına çevir
  # Önce mevcut URI'leri kontrol et ve eşleşeni bul
  AVAILABLE_URIS=$(curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | \
    python3 -c "import sys, json; data=json.load(sys.stdin); \
    uris=[tag['values'] for tag in data.get('availableTags', []) if tag.get('tag') == 'uri'][0] if 'availableTags' in data else []; \
    print('\n'.join(uris))" 2>/dev/null)
  
  # Endpoint pattern'lerini kontrol et
  if echo "$AVAILABLE_URIS" | grep -q "^${ENDPOINT%/*}/"; then
    # Eşleşen pattern bulundu
    METRIC_URI=$(echo "$AVAILABLE_URIS" | grep "^${ENDPOINT%/*}/" | head -1)
  else
    # Basit dönüşüm: son segment'i {slug} veya {id} ile değiştir
    if [[ "$ENDPOINT" == *"/pages/"* ]]; then
      METRIC_URI=$(echo "$ENDPOINT" | sed 's|/[^/]*$|/{slug}|')
    elif [[ "$ENDPOINT" == *"/posts/"* ]] || [[ "$ENDPOINT" == *"/components/"* ]] || [[ "$ENDPOINT" == *"/widgets/"* ]]; then
      METRIC_URI=$(echo "$ENDPOINT" | sed 's|/[^/]*$|/{id}|')
    else
      METRIC_URI=$(echo "$ENDPOINT" | sed 's|/[^/]*$|/{id}|')
    fi
  fi
  
  echo "📝 Metrik URI: $METRIC_URI"
else
  # Zaten metrik formatında (örn: /api/v1/pages/{slug})
  METRIC_URI=$ENDPOINT
fi

echo "=========================================="
echo "📊 Endpoint Metrikleri: $ENDPOINT"
if [ ! -z "$TOKEN" ]; then
  echo "🔐 Token kullanılıyor"
fi
echo "=========================================="
echo ""

# Metrik URI'sini kullan (eğer gerçek path verildiyse, metrik formatına çevrildi)
METRIC_URI_TO_USE=${METRIC_URI:-$ENDPOINT}

# Metrikleri çek
RESPONSE=$(curl -s -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:$METRIC_URI_TO_USE")

if [ $? -ne 0 ]; then
  echo "❌ Metrikler alınamadı. Uygulama çalışıyor mu?"
  exit 1
fi

# JSON parse et
echo "$RESPONSE" | python3 -m json.tool

echo ""
echo "=========================================="
echo "📈 Özet:"
echo "=========================================="

# COUNT
COUNT=$(echo "$RESPONSE" | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))' 2>/dev/null)
echo "Toplam İstek Sayısı: $COUNT"

# TOTAL_TIME
TOTAL_TIME=$(echo "$RESPONSE" | python3 -c 'import sys, json; print(json.load(sys.stdin)["measurements"][1]["value"])' 2>/dev/null)
echo "Toplam Süre: ${TOTAL_TIME}s ($(echo "$TOTAL_TIME * 1000" | bc | cut -d. -f1)ms)"

# MAX
MAX=$(echo "$RESPONSE" | python3 -c 'import sys, json; print(json.load(sys.stdin)["measurements"][2]["value"])' 2>/dev/null)
echo "En Uzun Süre: ${MAX}s ($(echo "$MAX * 1000" | bc | cut -d. -f1)ms)"

# Ortalama
if [ ! -z "$COUNT" ] && [ "$COUNT" != "0" ]; then
  AVG=$(echo "scale=4; $TOTAL_TIME / $COUNT" | bc)
  AVG_MS=$(echo "$AVG * 1000" | bc | cut -d. -f1)
  echo "Ortalama Süre: ${AVG}s (${AVG_MS}ms)"
fi

echo ""
