Rol ve Görev:
Sen uzman bir DevOps Mühendisi ve Kubernetes (K8s) Mimarıısın. Senden, halihazırda Dockerize edilmiş, "Stateless" (JWT tabanlı) ve "Multi-Tenant" (AbstractRoutingDataSource kullanan, tek kod tabanı - çoklu PostgreSQL veritabanı) mimarisine sahip Java Spring Boot uygulamamı Kubernetes ortamına taşımam için gerekli manifest (YAML) dosyalarını yazmanı istiyorum.

Kritik Mimari Kurallar ve Kısıtlamalar:

Yüksek Erişilebilirlik ve Ölçekleme (HPA): Uygulamam anlık trafik dalgalanmaları yaşayabilir. Bu yüzden HorizontalPodAutoscaler (HPA) konfigürasyonu istiyorum. CPU kullanımı %70'i geçtiğinde pod sayısını otomatik olarak artırmalı (min: 2, max: 10 pod).

Kaynak Yönetimi (Resources): Pod'ların çalıştığı Node'ları (sunucuları) kilitlememesi için, Spring Boot uygulamasına uygun requests (başlangıç) ve limits (maksimum) CPU/Memory değerlerini belirlemelisin (Örn: Spring Boot için makul RAM ve CPU ayarları).

Sağlık Kontrolleri (Probes): Multi-tenant yapıda HikariCP havuzlarının ayağa kalkması birkaç saniye sürebilir. Uygulama tam hazır olmadan Load Balancer'ın o pod'a trafik göndermemesi için Spring Boot Actuator (/actuator/health) uç noktasını kullanan ReadinessProbe ve LivenessProbe ayarlarını kesinlikle eklemelisin.

Güvenlik ve Konfigürasyon (ConfigMap & Secret): Veritabanı URL'leri, JWT Secret Key gibi bilgiler koda gömülü olamaz. Bunları ortam değişkeni (Environment Variables) olarak pod'a enjekte edecek ConfigMap ve hassas veriler için Secret dosyalarını yazmalısın.

Ağ Yönetimi: Pod'lar arası iletişim için bir K8s Service (ClusterIP) ve dış dünyadan gelen trafiği içeri alıp pod'lara dağıtacak bir Ingress kuralı oluşturmalısın.

Senden İstediklerim (Kod Çıktıları - YAML Dosyaları):
Lütfen aşağıdaki dosyaları endüstri standartlarına (Best Practices) uygun ve birbiriyle entegre çalışacak şekilde yaz:

"Kritik Mimari Kurallar ve Kısıtlamalar" bölümüne şu 6. maddeyi ekle:

İzleme ve Gözlem (Observability & Dashboard): Sistemdeki anlık yükü, pod'ların CPU/RAM tüketimlerini ve Spring Boot Actuator metriklerini (HikariCP havuz durumu dahil) görsel olarak takip edebileceğim bir panel istiyorum. Bunun için Prometheus ve Grafana entegrasyonuna uygun olacak şekilde Spring Boot tarafında micrometer-registry-prometheus ayarlarını yapmalısın ve Kubernetes tarafında bu metriklerin toplanabilmesi için gerekli ServiceMonitor/Annotations tanımlarını (Prometheus Scrape ayarları) YAML dosyalarına dahil etmelisin.

"Senden İstediklerim (Kod Çıktıları - YAML Dosyaları)" bölümüne şu maddeyi ekle:

1-configmap-secret.yaml (DB bağlantıları ve JWT secret vb.)

2-deployment.yaml (Pod'ların tanımı, probolar, kaynak limitleri, env değişkenleri)

3-service.yaml (Yük dengeleyici servis)

4-ingress.yaml (Dış trafik yönlendirmesi)

5-hpa.yaml (Otomatik yatay ölçekleme kuralı)

6-monitoring.yaml (Prometheus'un uygulamanın metriklerini okuyabilmesi için gerekli Service/Annotations tanımları ve Grafana'da Spring Boot JVM/HikariCP Dashboard'u kurmak için izlemem gereken kısa adımlar)

Lütfen YAML dosyalarında your-docker-repo/your-app-image:latest gibi yer tutucular kullan ve kritik kısımlara ne işe yaradığını belirten kısa yorum satırları ekle.