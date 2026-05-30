# Sunucu (GCP / K8s) — Durum & Drift Denetimi

> **Bağlam:** Bu not, repo'daki `k8s/` configleri ile `kubectl get pods -n elly`
> çıktısının karşılaştırmasıdır. Bu denetim sırasında canlı cluster'a **doğrudan
> erişim yoktu**; bulgular repo + paylaşılan pod listesine dayanır. Kesinleştirmek
> için §7'deki "doğrula" komutlarını çalıştır.
> **Bilgi anı:** 2026-05-30 — test/prod, gerçek kullanıcı trafiği yok.

## 0. TL;DR
- Canlıda **1 `elly-app` pod**, repo `replicas: 2` → **DRIFT**.
- **Burst deployment + HPA** repo'da var ama canlıda **burst pod yok** → autoscaling
  muhtemelen UYGULANMAMIŞ (yük artınca ekstra pod **açılmaz**).
- **zipkin + redisinsight** monitoring'de tanımlı, canlıda **yok**.
- **İki k8s config seti** var (`k8s/` ve `k8s/k8s/`) ve **drift etmişler** → tek kaynak belirsiz.
- **rabbitmq** 18 restart (instabilite işareti; email infra, chat'i etkilemez).

## 1. Gözlemlenen pod'lar (`kubectl get pods -n elly`)
| Pod | Durum | Not |
|---|---|---|
| elly-app-… | Running 1/1 | **tek** base pod (AGE ~2dk → son deploy) |
| postgres-basedb-0 / tenant1-0 / tenant2-0 | Running | OK (3× StatefulSet) |
| redis-0 | Running | OK |
| rabbitmq-0 | Running, **RESTARTS 18** | bkz. B6 |
| grafana, prometheus | Running | OK |
| _(yok)_ elly-app-burst | — | beklenen değil ama HPA min1 ise 1 olmalıydı → bkz. B3 |
| _(yok)_ zipkin | — | tracing → bkz. B4 |
| _(yok)_ redisinsight | — | Redis GUI → bkz. B4 |

## 2. Repo config ↔ Canlı uyum
| Kaynak | Repo (k8s/) | Canlı (gözlem) | Durum |
|---|---|---|---|
| elly-app | `replicas: 2` | 1 pod | ⚠️ DRIFT |
| elly-app-burst | `replicas: 0` + HPA(min1,max3) | 0 pod | ⚠️ HPA/burst apply edilmemiş gibi |
| postgres ×3 | replicas:1 | 3 pod | ✅ |
| redis | replicas:1 | 1 pod | ✅ |
| rabbitmq | replicas:1 | 1 pod (18 restart) | ⚠️ |
| grafana | replicas:1 | 1 pod | ✅ |
| prometheus | replicas:1 | 1 pod | ✅ |
| zipkin | replicas:1 | yok | ⚠️ EKSİK |
| redisinsight | replicas:1 | yok | ⚠️ EKSİK |

## 3. Bulgular

### B1 — İki config seti drift etmiş ⚠️ (kök karışıklık)
- `k8s/*.yaml` ve `k8s/k8s/*.yaml` → aynı 12 dosyanın iki kopyası. Farklı olanlar:
  - `2a-app-deployment`: üst `k8s/`'te fazladan `timeoutSeconds: 5` (probe) var.
  - `4-ingress`: üst `k8s/` **`admin.huseyindol.com`** host + path'lerini içeriyor; `k8s/k8s/` **İÇERMİYOR**.
- admin.huseyindol.com çalıştığına göre uygulanan ingress muhtemelen **üst `k8s/`**'ten geldi
  → "sunucu ayarları k8s/k8s/" varsayımı en azından ingress için yanlış.
- **Risk:** hangi set apply edilecek belirsiz; yanlış set → admin routing kaybı / replicas değişimi.
- **Öneri:** TEK kaynak seç (muhtemelen üst `k8s/` daha güncel), diğerini sil/arşivle.

### B2 — elly-app replicas: repo 2, canlı 1 ⚠️
- Repo (her iki set) `replicas: 2`. Canlı: 1 pod.
- Senin deploy davranışın (1 yeni pod aç → eskini kapat → 1 pod kal) ve niyetin
  (1 base + trafikte burst) aslında **replicas:1** demek. replicas:2 ile steady-state **2 pod** olurdu.
- **Karar gerek:**
  - (A) 1 base + burst (söylediğin) → repo'da base **replicas:1** olmalı (şu an 2 = drift/hata).
  - (B) 2 base (HA) → canlıda neden 1? (tek VM, 2. pod schedule olamıyor olabilir) + chat çok-pod riski (§5).
- Niyet (A) ise: `2a-app-deployment.yaml` → `replicas: 1`.

### B3 — Burst + HPA uygulanmamış → autoscaling PASİF ⚠️
- Repo: `elly-app-burst` (replicas:0) + `elly-burst-hpa` (min1, max3, hedef=burst, base CPU>700m'de tetik).
- HPA min1 olduğundan UYGULANMIŞ olsa **en az 1 burst pod** beklenirdi. Canlıda yok → burst deployment ve/veya HPA **apply edilmemiş**.
- **Etki:** "Trafik çoğalınca ekstra pod açılacak" beklentin **şu an çalışmıyor**. Yük gelirse tek pod ezilir, otomatik ölçeklenmez.
- **Doğrula:** `kubectl get deploy,hpa -n elly`

### B4 — zipkin + redisinsight eksik
- `6-monitoring.yaml` 4 deployment tanımlıyor: prometheus, grafana, **zipkin**, **redisinsight**. Canlıda ilk ikisi var.
- **Etki:** distributed tracing (`application.properties` → `management.zipkin.tracing.endpoint`) ve Redis GUI çalışmıyor. App zipkin'e span gönderemeyip log gürültüsü üretebilir; fonksiyonel kritik değil.
- **Çözüm:** istiyorsan `kubectl apply -f k8s/6-monitoring.yaml`; istemiyorsan monitoring'den çıkar (gereksiz tanım kalmasın).

### B5 — ingress drift (admin host) ⚠️
- (B1 ayrıntısı.) `k8s/k8s/4-ingress.yaml` admin.huseyindol.com host bloğunu içermiyor; üst `k8s/` içeriyor. Yanlış set apply edilirse panel erişimi kırılır.

### B6 — rabbitmq 18 restart
- 53 günde 18 restart → ara ara çöküyor (muhtemel OOM / probe fail). Email kuyruğu burada; **chat etkilenmez** (chat in-memory broker).
- **Doğrula:** `kubectl describe pod rabbitmq-0 -n elly | grep -iA3 "Last State"` (OOMKilled?), `kubectl logs rabbitmq-0 -n elly --previous`

## 4. "2 pod olmalı mıydı?" — net cevap
- **Repo'ya göre:** evet (base replicas:2 → 2 pod). Canlı 1 → drift.
- **Niyetine göre:** hayır — 1 base + trafikte burst istiyorsan base **replicas:1** olmalı. Canlı (1) niyetinle uyumlu, repo (2) değil.
- **Sonuç:** çelişki repo'da. Niyet 1 base ise repo'yu 1'e çek → drift biter, kafa karışıklığı kalkar.

## 5. ⚠️ Chat ↔ autoscaling gerilimi (kritik — akılda tut)
- Chat şu an **in-memory broker** + **sticky session YOK**. Tek pod'da sorunsuz.
- Burst/HPA düzeltilip yük altında 2-3 pod açılırsa **chat bozulur**: pod'lar arası mesaj gitmez
  (her pod kendi broker'ı) + SockJS oturumu pod'lar arası kopar (`xhr_send` yanlış pod'a düşer).
- Yani "autoscaling'i çalıştır" ile "chat güvenilir" **şu an çelişir**. Çok-pod'a geçmeden önce gerekenler:
  1. **Sticky session** (service `sessionAffinity: ClientIP` veya ingress sticky-cookie)
  2. **Shared broker** (RabbitMQ STOMP relay veya Redis relay → `enableStompBrokerRelay`)
- Geçici güvenli mod (şu anki fiili durum): **base 1 pod + burst kapalı** → chat hep tek pod.

## 6. Öncelikli yapılacaklar
1. **Tek config kaynağı** belirle (`k8s/` muhtemelen güncel), diğerini arşivle/sil → drift biter.
2. base `replicas`'ı niyete göre sabitle (1 base + burst istiyorsan **1**).
3. Autoscaling kararı: ya burst+HPA'yı apply et **ama önce chat için sticky+broker**, ya da bilinçli tek-pod'da kal.
4. (Opsiyonel) zipkin/redisinsight: apply et ya da monitoring'den çıkar.
5. rabbitmq restart sebebini incele (OOM ise memory limit artır).

## 7. Doğrulama komutları (canlıda çalıştır, bu notu güncelle)
```bash
kubectl get deploy,sts,hpa,pods -n elly -o wide
kubectl get hpa -n elly                              # burst HPA gerçekten var mı?
kubectl describe deploy elly-app -n elly | grep -i replicas   # desired vs available
kubectl get pods -n elly --field-selector=status.phase=Pending  # schedule olamayan var mı?
kubectl describe pod rabbitmq-0 -n elly | grep -iA3 "Last State"
kubectl get ingress -n elly -o yaml | grep -iE "host:"
```

---

## 8. Olay kaydı — 2026-05-30: rabbitmq deprecated env → elly-app cascade

**Belirti:** elly-app + rabbitmq ikisi de CrashLoopBackOff (prod down).

**Kök sebep (zincir):**
1. rabbitmq StatefulSet'inde `RABBITMQ_VM_MEMORY_HIGH_WATERMARK` (+ `RABBITMQ_DISK_FREE_LIMIT`)
   **deprecated env**'leri set'liydi. RabbitMQ **3.13** bunları **reddedip çöküyor** ("set but deprecated").
2. rabbitmq hiç Ready olmayınca headless service'in endpoint'i boş → `rabbitmq` DNS'i **çözülmüyor**.
3. elly-app boot'ta AMQP'ye bağlanırken `UnknownHostException: rabbitmq` → context init fail →
   **CrashLoopBackOff**. (Yani elly-app çöküşü rabbitmq'ya bağımlıydı — bağımsız değil.)

**Çözüm (uygulandı, canlıda):**
```bash
kubectl set env statefulset/rabbitmq RABBITMQ_VM_MEMORY_HIGH_WATERMARK- -n elly
kubectl delete pod rabbitmq-0 -n elly        # yeni spec'le başlat
# rabbitmq 1/1 olunca elly-app backoff retry'ında kendi toparladı
```

**Kalıcı yapılacak (canlı manuel düzeltildi → repo ile DRIFT var):**
- `k8s/2e-rabbitmq.yaml`'ın conf-tabanlı sürümünü (deprecated env YOK + `rabbitmq.conf` ile
  watermark) **commit + apply** et. `RABBITMQ_DISK_FREE_LIMIT` hâlâ canlıda set (deprecated);
  proper apply onu da temizler.
- **elly-app dayanıklılığı:** rabbitmq DNS yokken boot'ta çökmemeli (AMQP bağlantısını lazy yap /
  queue declare'i fail-safe yap). Yoksa rabbitmq her düştüğünde app de düşer.

**Ders:** RabbitMQ 3.13+ ile `RABBITMQ_*` deprecated env KULLANMA → `rabbitmq.conf`. Ve app'i tek
bir bağımlılığın (rabbitmq DNS) boot'ta çökertmesine izin verme.

> Yan gözlem: `cm-acme-http-solver-*` pod'u saatlerce duruyorsa TLS sertifika (Let's Encrypt
> HTTP-01) issuance'ı takılmış olabilir — acil değil, ingress/DNS ile ayrıca bakılır.

---
_Not: Bu denetim repo + `kubectl get pods` çıktılarına dayanır. §7 komutlarıyla canlıyı teyit et._
