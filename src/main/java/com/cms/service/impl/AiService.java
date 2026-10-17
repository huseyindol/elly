package com.cms.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.cms.config.TenantContext;
import com.cms.dto.DtoAiAltTextResponse;
import com.cms.dto.DtoAiArticleRequest;
import com.cms.dto.DtoAiArticleResponse;
import com.cms.dto.DtoAiBlogInfoRequest;
import com.cms.dto.DtoAiBlogInfoResponse;
import com.cms.dto.DtoAiDescriptionRequest;
import com.cms.dto.DtoAiDescriptionResponse;
import com.cms.dto.DtoAiSeoResponse;
import com.cms.dto.DtoAiSlugResponse;
import com.cms.dto.DtoAiTitleRequest;
import com.cms.dto.DtoAiUsageLog;
import com.cms.dto.DtoAiUsageStats;
import com.cms.dto.DtoAiUsageStats.DtoAiActionStat;
import com.cms.entity.AiUsageLog;
import com.cms.exception.BadRequestException;
import com.cms.exception.BaseException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.AiUsageLogRepository;
import com.cms.service.IAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini Interactions API (/v1/interactions) üzerinden AI içerik üretim servisi.
 * Model varsayılan olarak {@code gemini-3.8-flash} kullanır.
 * API anahtarı sadece backend'de tutulur ve hata mesajlarında asla dışarı sızdırılmaz.
 * Her Gemini alt çağrısı (istek, prompt, yanıt, token kırılımı, süre, durum) ai_usage_logs tablosuna kaydedilir.
 */
@Service
@Slf4j
public class AiService implements IAiService {

  private static final String INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions";
  private static final int MAX_PLAIN_CONTENT_LEN = 4000;

  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String apiKey;
  private final String modelName;
  private final AiUsageLogRepository aiUsageLogRepository;
  private final AiUsageLogWriter aiUsageLogWriter;

  @Autowired
  public AiService(
      ObjectMapper objectMapper,
      AiUsageLogRepository aiUsageLogRepository,
      AiUsageLogWriter aiUsageLogWriter,
      @Value("${app.ai.gemini.api-key:}") String apiKey,
      @Value("${app.ai.gemini.model:gemini-3.8-flash}") String modelName,
      @Value("${app.ai.gemini.connect-timeout-ms:10000}") int connectTimeoutMs,
      @Value("${app.ai.gemini.read-timeout-ms:90000}") int readTimeoutMs) {
    this(
        objectMapper,
        createDefaultRestClient(connectTimeoutMs, readTimeoutMs),
        apiKey,
        modelName,
        aiUsageLogRepository,
        aiUsageLogWriter);
  }

  public AiService(
      ObjectMapper objectMapper,
      RestClient restClient,
      String apiKey,
      String modelName) {
    this(objectMapper, restClient, apiKey, modelName, null, null);
  }

  public AiService(
      ObjectMapper objectMapper,
      RestClient restClient,
      String apiKey,
      String modelName,
      AiUsageLogRepository aiUsageLogRepository,
      AiUsageLogWriter aiUsageLogWriter) {
    this.objectMapper = objectMapper;
    this.restClient = restClient;
    this.apiKey = apiKey != null ? apiKey.trim() : "";
    this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "gemini-3.8-flash";
    this.aiUsageLogRepository = aiUsageLogRepository;
    this.aiUsageLogWriter = aiUsageLogWriter;
  }

  private static RestClient createDefaultRestClient(int connectTimeoutMs, int readTimeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public DtoAiSlugResponse generateSlug(DtoAiTitleRequest request) {
    String title = requireNonBlank(request.getTitle(), "Başlık alanı zorunludur");
    log.info("[AiService] Slug üretiliyor: titleLen={}", title.length());

    String prompt = """
        Aşağıdaki Türkçe başlıktan SEO dostu bir URL slug'ı oluştur.
        Kurallar:
        - Sadece küçük harf, rakam ve tire (-) kullan
        - Türkçe karakterleri dönüştür (ş→s, ğ→g, ü→u, ö→o, ı→i, ç→c)
        - Boşlukları tire ile değiştir
        - Ardışık tireleri tekleştir
        - Maksimum 80 karakter
        - Sadece slug'ı döndür, başka hiçbir şey yazma

        Başlık: %s""".formatted(title);

    GeminiCallResult call = callGemini(
        new GeminiCallSpec(newRequestId(), 1, "/api/v1/ai/slug", "slug", toJsonSafe(Map.of("title", title)), prompt));
    String slug = call.text().trim()
        .replace('İ', 'i')
        .replace('I', 'i')
        .replace('ı', 'i')
        .replace('Ş', 's')
        .replace('ş', 's')
        .replace('Ğ', 'g')
        .replace('ğ', 'g')
        .replace('Ü', 'u')
        .replace('ü', 'u')
        .replace('Ö', 'o')
        .replace('ö', 'o')
        .replace('Ç', 'c')
        .replace('ç', 'c')
        .toLowerCase(java.util.Locale.ROOT)
        .replaceAll("[\\s_]+", "-")
        .replaceAll("[^a-z0-9-]", "")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");

    if (slug.length() > 80) {
      slug = slug.substring(0, 80).replaceAll("-+$", "");
    }
    if (slug.isBlank()) {
      recordParseFailure(call.logEntry(), "EMPTY_SLUG", "AI geçerli bir slug üretemedi.");
      throw new AiServiceException("AI geçerli bir slug üretemedi.");
    }

    DtoAiSlugResponse result = DtoAiSlugResponse.builder().slug(slug).build();
    recordSuccess(call.logEntry(), toJsonSafe(result));
    return result;
  }

  @Override
  public DtoAiSeoResponse generateSeoFields(DtoAiTitleRequest request) {
    String title = requireNonBlank(request.getTitle(), "Başlık alanı zorunludur");
    log.info("[AiService] SEO alanları üretiliyor: titleLen={}", title.length());

    String prompt = """
        Aşağıdaki başlık için SEO meta verilerini Türkçe olarak oluştur.
        Başlık: "%s"

        Tam olarak şu JSON formatında yanıt ver (başka hiçbir şey yazma):
        {
          "seoTitle": "max 60 karakter SEO başlığı",
          "seoDescription": "max 155 karakter meta açıklaması, kullanıcıyı tıklamaya teşvik edecek",
          "seoKeywords": "anahtar1, anahtar2, anahtar3, anahtar4, anahtar5"
        }""".formatted(title);

    GeminiCallResult call = callGemini(
        new GeminiCallSpec(newRequestId(), 1, "/api/v1/ai/seo", "seo", toJsonSafe(Map.of("title", title)), prompt));
    String raw = stripCodeFences(call.text());
    try {
      JsonNode node = objectMapper.readTree(raw);
      DtoAiSeoResponse result = DtoAiSeoResponse.builder()
          .seoTitle(node.path("seoTitle").asText("").trim())
          .seoDescription(node.path("seoDescription").asText("").trim())
          .seoKeywords(node.path("seoKeywords").asText("").trim())
          .build();
      recordSuccess(call.logEntry(), toJsonSafe(result));
      return result;
    } catch (Exception e) {
      log.error("[AiService] SEO JSON ayrıştırma hatası", e);
      recordParseFailure(call.logEntry(), "PARSE_ERROR", "SEO alanları üretilirken yanıt ayrıştırılamadı.");
      throw new AiServiceException("SEO alanları üretilirken yanıt ayrıştırılamadı.");
    }
  }

  @Override
  public DtoAiDescriptionResponse generateDescription(DtoAiDescriptionRequest request) {
    String name = requireNonBlank(request.getName(), "Ad/başlık alanı zorunludur");
    String context = (request.getContext() != null && !request.getContext().isBlank())
        ? request.getContext().trim()
        : "içerik";
    log.info("[AiService] Açıklama üretiliyor: context={}, nameLen={}", context, name.length());

    String prompt = """
        Aşağıdaki %s adı için kısa ve açıklayıcı bir Türkçe açıklama yaz.
        Ad: "%s"

        Kurallar:
        - Maksimum 2 cümle
        - Türkçe yaz
        - Sadece açıklamayı döndür, başka hiçbir şey yazma""".formatted(context, name);

    GeminiCallResult call = callGemini(new GeminiCallSpec(
        newRequestId(),
        1,
        "/api/v1/ai/description",
        "description",
        toJsonSafe(Map.of("name", name, "context", context)),
        prompt));
    String description = call.text().trim();
    DtoAiDescriptionResponse result = DtoAiDescriptionResponse.builder().description(description).build();
    recordSuccess(call.logEntry(), toJsonSafe(result));
    return result;
  }

  @Override
  public DtoAiAltTextResponse generateAltText(DtoAiTitleRequest request) {
    String title = requireNonBlank(request.getTitle(), "Başlık alanı zorunludur");
    log.info("[AiService] Alt metin üretiliyor: titleLen={}", title.length());

    String prompt = """
        Aşağıdaki başlık için bir banner görselinin SEO dostu alt metnini yaz.
        Başlık: "%s"

        Kurallar:
        - Maksimum 10 kelime
        - Görseli kısaca açıkla, anahtar kelime içersin
        - Türkçe yaz
        - Sadece alt metni döndür, başka hiçbir şey yazma""".formatted(title);

    GeminiCallResult call = callGemini(new GeminiCallSpec(
        newRequestId(), 1, "/api/v1/ai/alt-text", "alt-text", toJsonSafe(Map.of("title", title)), prompt));
    String altText = call.text().trim();
    DtoAiAltTextResponse result = DtoAiAltTextResponse.builder().altText(altText).build();
    recordSuccess(call.logEntry(), toJsonSafe(result));
    return result;
  }

  @Override
  public DtoAiBlogInfoResponse generateBlogInfo(DtoAiBlogInfoRequest request) {
    String title = requireNonBlank(request.getTitle(), "Başlık alanı zorunludur");
    String plainContent = stripHtmlForPrompt(request.getContent(), MAX_PLAIN_CONTENT_LEN);
    if (plainContent.isBlank()) {
      throw new BadRequestException("Başlık ve içerik gerekli");
    }

    log.info("[AiService] Blog meta üretiliyor: titleLen={}, contentLen={}", title.length(), plainContent.length());

    String prompt = """
        Aşağıdaki blog yazısının başlığı ve içeriğine göre meta bilgileri üret.

        Başlık: "%s"
        İçerik:
        \"\"\"
        %s
        \"\"\"

        Çıktı kuralları:
        - description: İçeriği özetleyen, kullanıcıyı meraklandıran, tek cümlelik Türkçe özet. Maksimum 180 karakter. Soru cümlesi olabilir.
        - category: İçeriğin ait olduğu kısa Türkçe kategori adı (1-3 kelime). Örnek: "Backend Güvenlik", "Frontend", "DevOps", "Yapay Zeka", "Veritabanı", "Mobil".
        - readingTime: İçeriği yaklaşık 200 kelime/dk hızla okuma süresi. Format: "X dk okuma" (örn: "4 dk okuma").
        - imagePrompt: İçeriği temsil eden, fotogerçekçi bir kapak görseli için kısa İNGİLİZCE görsel prompt'u. Maksimum 14 kelime, kişisel veya marka isimleri içermesin, "blog cover, modern, professional" gibi stilistik ipuçları içerebilir.

        Tam olarak şu JSON formatında yanıt ver, başka hiçbir şey yazma:
        {
          "description": "...",
          "category": "...",
          "readingTime": "X dk okuma",
          "imagePrompt": "..."
        }""".formatted(title, plainContent);

    GeminiCallResult call = callGemini(new GeminiCallSpec(
        newRequestId(),
        1,
        "/api/v1/ai/blog-info",
        "blog-info",
        toJsonSafe(Map.of("title", title, "contentPreview", truncate(plainContent, 500), "contentChars", plainContent.length())),
        prompt));
    String raw = stripCodeFences(call.text());
    try {
      JsonNode node = objectMapper.readTree(raw);
      DtoAiBlogInfoResponse result = DtoAiBlogInfoResponse.builder()
          .description(node.path("description").asText("").trim())
          .category(node.path("category").asText("").trim())
          .readingTime(node.path("readingTime").asText("").trim())
          .imagePrompt(node.path("imagePrompt").asText("").trim())
          .build();
      recordSuccess(call.logEntry(), toJsonSafe(result));
      return result;
    } catch (Exception e) {
      log.error("[AiService] BlogInfo JSON ayrıştırma hatası", e);
      recordParseFailure(call.logEntry(), "PARSE_ERROR", "Blog bilgileri üretilirken yanıt ayrıştırılamadı.");
      throw new AiServiceException("Blog bilgileri üretilirken yanıt ayrıştırılamadı.");
    }
  }

  @Override
  public DtoAiArticleResponse generateArticle(DtoAiArticleRequest request) {
    String topic = requireNonBlank(request.getTopic(), "Makale konusu zorunludur");
    if (topic.length() < 3) {
      throw new BadRequestException("Makale konusu en az 3 karakter olmalıdır");
    }
    String keywords = request.getKeywords() != null ? request.getKeywords().trim() : "";

    log.info("[AiService] 3-aşamalı makale üretimi başlatıldı: topicLen={}", topic.length());
    String requestId = newRequestId();
    String userInputJson = toJsonSafe(Map.of("topic", topic, "keywords", keywords));

    // Step 1: Outline Agent
    String outlinePrompt = """
        Sen bir makale planlama ve içerik stratejisi uzmanısın.
        Görevin: Verilen ham konu ve anahtar kelimeleri alarak, AI'ın derinlemesine, görsel destekli ve odaklı bir makale yazabilmesi için yapılandırılmış bir plan oluşturmak.

        Ham Konu: %s
        Yardımcı Anahtar Kelimeler: %s

        Lütfen aşağıdaki formatta bir makale planı oluştur:
        - Ana başlık önerisi
        - Hedef kitle
        - Makalenin ana mesajı (1-2 cümle)
        - Bölüm başlıkları (en az 5, en fazla 7 bölüm) — her bölüm derinlemesine işlenecek
        - Her bölümde ele alınacak temel noktalar (3-5 madde, sadece liste değil; kavram, örnek ve gerekçe)
        - Görsel planı: Makale gövdesi içine yerleştirilecek 2 (en fazla 3) görselin nereye konulacağı ve içeriği. Her görsel için:
          * yerleşim: "Hangi bölümün sonunda / hangi paragrafın altında"
          * imagePrompt: kısa İNGİLİZCE görsel prompt (max 14 kelime, fotogerçekçi/diyagram/illustration stilini belirt, marka/kişi adı içermesin)
          * alt: kısa Türkçe alt metin (max 10 kelime)
        - Sonuç bölümünde yer alacak çağrı-eylem (CTA)

        Yanıtını Türkçe ver ve yalnızca yapılandırılmış planı döndür, ek açıklama yapma."""
        .formatted(topic, keywords.isEmpty() ? "Belirtilmedi" : keywords);

    GeminiCallResult outlineCall = callGemini(
        new GeminiCallSpec(requestId, 1, "/api/v1/ai/article", "article:outline", userInputJson, outlinePrompt));
    String outline = outlineCall.text();
    recordSuccess(outlineCall.logEntry(), outline);
    log.info("[AiService] Makale outline hazırlandı");

    // Step 2: Full Article Agent
    String articlePrompt = """
        Sen profesyonel bir Türkçe içerik yazarı ve web editörüsün.
        Görevin: Aşağıdaki makale planına göre DERİNLEMESİNE, görsel destekli ve SEO dostu bir makale yazmak.

        MAKALE PLANI:
        %s

        YAZIM KURALLARI:
        - Makaleyi Türkçe yaz
        - Akıcı, anlaşılır ve profesyonel bir dil kullan
        - Yüzeysel geçme: her bölüm derinlemesine işlensin, kavramlar tanımlansın, gerekçelendirilsin, somut örnek/senaryo/karşılaştırma verilsin
        - Toplam uzunluk minimum 800, maksimum 1500 kelime olmalı (bölüm başına ortalama 150-250 kelime). 800'ün altı kabul edilmez, 1500'ü aşma.
        - Sadece geçerli HTML çıktısı ver (body içeriği, html/body/head etiketleri olmadan)
        - Kullanabileceğin HTML etiketleri: h1, h2, h3, p, ul, ol, li, strong, em, blockquote, img
        - Başlıklar için h1 (ana başlık, sadece 1 adet), h2 (bölüm başlıkları), h3 (alt başlıklar) kullan
        - Önemli kavramları <strong> ile vurgula
        - Listeleri yalnızca açıklayıcı olduğu yerde kullan — gereksiz bullet'a kaçma

        GÖRSEL KURALLARI (ZORUNLU):
        - Makale gövdesine 2 (gerekiyorsa en fazla 3) görsel yerleştir. Daha azı kabul edilmez, daha fazlası dağıtıcı olur.
        - Her görsel ayrı bir <p>...</p> bloğu içinde olmalı: <p><img src="..." alt="..." width="1200" height="675" loading="lazy" /></p>
        - Görseller H1 başlığından ÖNCE olmaz. İlk görsel ilk veya ikinci bölümün ardından, son görsel sonuç bölümünden ÖNCE olmalı.
        - Her img src değeri ŞU FORMATTA olmalı (başka domain kullanma):
          https://image.pollinations.ai/prompt/{ENCODED_PROMPT}?width=1200&height=675&nologo=true
          - {ENCODED_PROMPT} kısmı URL-encoded İngilizce görsel prompt'tur (boşluk yerine %%20, virgül yerine %%2C)
          - Görsel prompt'u 6-14 kelime, fotogerçekçi/illustration/diagram stili belirt, marka veya kişi adı içermesin
        - Her görselin alt metni TÜRKÇE, max 12 kelime, görseli açıklayıcı ve anahtar kavramı barındıran şekilde olmalı
        - Görsel için ek başlık/figcaption yazma — sadece <p><img/></p>

        Herhangi bir markdown, ek açıklama veya kod bloğu ekleme — sadece saf HTML döndür."""
        .formatted(outline);

    GeminiCallResult draftCall = callGemini(
        new GeminiCallSpec(requestId, 2, "/api/v1/ai/article", "article:draft", userInputJson, articlePrompt));
    String rawHtml = draftCall.text();
    recordSuccess(draftCall.logEntry(), rawHtml);
    log.info("[AiService] Ham makale HTML üretildi");

    // Step 3: Frontend Review & Sanitization Agent
    String reviewPrompt = """
        Sen bir frontend geliştirici ve HTML/erişilebilirlik uzmanısın.
        Görevin: Verilen HTML makale içeriğini incelemek, düzeltmek ve temizlemek.

        HAM HTML:
        %s

        YAPILACAKLAR:
        1. Sadece şu etiketlere izin ver: h1, h2, h3, p, ul, ol, li, strong, em, blockquote, img — diğer tüm etiketleri kaldır
        2. Başlık hiyerarşisini düzelt (h1 → h2 → h3 sırası, sadece 1 adet h1)
        3. Boş etiketleri kaldır
        4. Birden fazla ardışık boş satırı tek satıra indir
        5. img dışındaki tüm etiketlerden script, style, class, id ve inline style attribute'larını kaldır
        6. img etiketinde SADECE şu attribute'lara izin ver: src, alt, width, height, loading. Diğerlerini (class, id, style, srcset, data-*, onerror vb.) kaldır.
        7. img src değeri "https://image.pollinations.ai/prompt/" ile başlamıyorsa ya da http/https ile başlayan geçerli bir URL değilse görseli komple kaldır.
        8. Her img için alt boşsa kısa Türkçe bir alt metni türet; img yine de <p>...</p> içinde sarmalanmış olmalı, değilse sarmalanmış hale getir
        9. Görsel sayısı 2 veya 3 olmalı. 1 veya 0 ise olduğu gibi bırak (daha fazlası dağıtıcı). 4+ görsel varsa fazlasını kaldır, içerikle en alakalı 3 tanesini koru.
        10. HTML entity'lerini düzgün encode et (&amp; &lt; &gt; vb.)
        11. Makul olmayan ya da çok kısa paragrafları bir önceki veya sonraki paragrafla birleştir

        ÖNEMLİ: Yalnızca temizlenmiş HTML döndür. Açıklama, yorum, markdown veya kod bloğu ekleme."""
        .formatted(rawHtml);

    GeminiCallResult sanitizeCall = callGemini(
        new GeminiCallSpec(requestId, 3, "/api/v1/ai/article", "article:sanitize", userInputJson, reviewPrompt));
    String cleanHtml = stripCodeFences(sanitizeCall.text());
    recordSuccess(sanitizeCall.logEntry(), cleanHtml);
    log.info("[AiService] Makale üretimi tamamlandı");
    return DtoAiArticleResponse.builder().html(cleanHtml).build();
  }

  // ==================== REPORTING & LOG LISTING ====================

  @Override
  @Transactional(readOnly = true)
  public Page<DtoAiUsageLog> listUsageLogs(
      String action, String status, String username, String q, Pageable pageable) {
    if (aiUsageLogRepository == null) {
      return Page.empty(pageable);
    }
    return aiUsageLogRepository
        .search(trimToNull(action), trimToNull(status), trimToNull(username), trimToNull(q), pageable)
        .map(AiService::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public DtoAiUsageLog getUsageLogById(Long id) {
    if (aiUsageLogRepository == null) {
      throw new ResourceNotFoundException("AI kullanım logu bulunamadı: " + id);
    }
    AiUsageLog entity = aiUsageLogRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AI kullanım logu bulunamadı: " + id));
    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public DtoAiUsageStats getUsageStats() {
    if (aiUsageLogRepository == null) {
      return DtoAiUsageStats.builder().byAction(List.of()).build();
    }
    List<Object[]> totalsRows = aiUsageLogRepository.aggregateTotals();
    Object[] totals = (totalsRows != null && !totalsRows.isEmpty()) ? totalsRows.get(0) : null;

    long totalCalls = toLong(totals, 0);
    long successCalls = toLong(totals, 1);
    long errorCalls = toLong(totals, 2);
    long totalInputTokens = toLong(totals, 3);
    long totalOutputTokens = toLong(totals, 4);
    long totalThoughtTokens = toLong(totals, 5);
    long totalCachedTokens = toLong(totals, 6);
    long totalTokens = toLong(totals, 7);
    double avgDurationMs = Math.round(toDouble(totals, 8) * 10.0) / 10.0;

    List<DtoAiActionStat> byAction = new ArrayList<>();
    for (Object[] row : aiUsageLogRepository.aggregateByAction()) {
      byAction.add(DtoAiActionStat.builder()
          .action(row[0] != null ? row[0].toString() : "")
          .endpoint(row[1] != null ? row[1].toString() : "")
          .callCount(toLong(row, 2))
          .successCount(toLong(row, 3))
          .errorCount(toLong(row, 4))
          .inputTokens(toLong(row, 5))
          .outputTokens(toLong(row, 6))
          .thoughtTokens(toLong(row, 7))
          .totalTokens(toLong(row, 8))
          .avgDurationMs(Math.round(toDouble(row, 9) * 10.0) / 10.0)
          .build());
    }

    return DtoAiUsageStats.builder()
        .totalCalls(totalCalls)
        .successCalls(successCalls)
        .errorCalls(errorCalls)
        .totalInputTokens(totalInputTokens)
        .totalOutputTokens(totalOutputTokens)
        .totalThoughtTokens(totalThoughtTokens)
        .totalCachedTokens(totalCachedTokens)
        .totalTokens(totalTokens)
        .avgDurationMs(avgDurationMs)
        .byAction(byAction)
        .build();
  }

  // ==================== INTERNAL HELPERS ====================

  private record GeminiCallSpec(
      String requestId,
      int stepOrder,
      String endpoint,
      String action,
      String userInput,
      String prompt) {}

  private record GeminiCallResult(String text, AiUsageLog logEntry) {}

  private GeminiCallResult callGemini(GeminiCallSpec spec) {
    long startNanos = System.nanoTime();
    AiUsageLog entry = AiUsageLog.builder()
        .requestId(spec.requestId())
        .stepOrder(spec.stepOrder())
        .endpoint(spec.endpoint())
        .action(spec.action())
        .model(modelName)
        .username(resolveCurrentUsername())
        .tenantId(TenantContext.getTenantId())
        .userInput(spec.userInput())
        .promptText(spec.prompt())
        .promptChars(spec.prompt() != null ? spec.prompt().length() : 0)
        .responseChars(0)
        .inputTokens(0)
        .outputTokens(0)
        .thoughtTokens(0)
        .cachedTokens(0)
        .totalTokens(0)
        .durationMs(0L)
        .status("ERROR")
        .build();

    if (apiKey.isBlank()) {
      recordError(
          entry,
          startNanos,
          null,
          "API_KEY_MISSING",
          "Gemini API anahtarı yapılandırılmamış. Lütfen sistem yöneticisiyle iletişime geçin.");
      throw new AiServiceException(
          "Gemini API anahtarı yapılandırılmamış. Lütfen sistem yöneticisiyle iletişime geçin.");
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", modelName);
    body.put("input", spec.prompt());
    body.put("store", false);

    try {
      String rawResponse = restClient.post()
          .uri(INTERACTIONS_URL)
          .header("x-goog-api-key", apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(String.class);

      long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
      entry.setDurationMs(elapsedMs);
      entry.setHttpStatus(200);

      if (rawResponse == null || rawResponse.isBlank()) {
        recordError(entry, startNanos, 200, "EMPTY_RESPONSE", "AI servisinden boş yanıt döndü.");
        throw new AiServiceException("AI servisinden boş yanıt döndü.");
      }

      JsonNode root = objectMapper.readTree(rawResponse);
      populateTokenUsage(entry, root);

      String extracted = extractTextFromInteraction(root);
      if (extracted == null || extracted.isBlank()) {
        recordError(entry, startNanos, 200, "EMPTY_TEXT", "AI servisinden metin çıktısı alınamadı.");
        throw new AiServiceException("AI servisinden metin çıktısı alınamadı.");
      }

      entry.setResponseText(extracted);
      entry.setResponseChars(extracted.length());
      entry.setStatus("SUCCESS");
      return new GeminiCallResult(extracted, entry);

    } catch (RestClientResponseException e) {
      int status = e.getStatusCode().value();
      String rawErrorBody = sanitizeErrorText(e.getResponseBodyAsString());
      if (rawErrorBody != null && !rawErrorBody.isBlank()) {
        entry.setResponseText(rawErrorBody);
        entry.setResponseChars(rawErrorBody.length());
      }
      String upstreamDetail = extractUpstreamErrorMessage(rawErrorBody);
      log.error("[AiService] Gemini API HTTP hatası: status={}, detail={}", status, upstreamDetail);

      if (status == 402) {
        String msg =
            "Gemini API ön ödemeli (prepay) kredisi tükenmiş (HTTP 402). Lütfen Google AI Studio (https://ai.studio/projects) üzerinden kredi yükleyin veya faturalandırmasız (Free Tier) bir proje API anahtarı kullanın.";
        recordError(
            entry,
            startNanos,
            status,
            "PREPAY_CREDITS_DEPLETED",
            upstreamDetail != null ? msg + " (" + upstreamDetail + ")" : msg);
        throw new AiServiceException(msg);
      }
      if (status == 400 || status == 401 || status == 403) {
        if (rawErrorBody != null
            && (rawErrorBody.contains("API_KEY")
                || rawErrorBody.toLowerCase().contains("api key")
                || status == 401
                || status == 403)) {
          String msg = "Gemini API anahtarı geçersiz veya yetkisiz. Lütfen sistem yöneticisiyle iletişime geçin.";
          recordError(
              entry,
              startNanos,
              status,
              "INVALID_API_KEY",
              upstreamDetail != null ? msg + " (" + upstreamDetail + ")" : msg);
          throw new AiServiceException(msg);
        }
      }
      if (status == 429) {
        String msg = "AI istek kotası aşıldı. Lütfen biraz bekleyip tekrar deneyin.";
        recordError(
            entry,
            startNanos,
            status,
            "RATE_LIMIT",
            upstreamDetail != null ? msg + " (" + upstreamDetail + ")" : msg);
        throw new AiServiceException(msg);
      }
      String msg =
          upstreamDetail != null
              ? "AI üretimi sırasında hata oluştu (HTTP " + status + "): " + upstreamDetail
              : "AI üretimi sırasında bir hata oluştu (HTTP " + status + ").";
      recordError(entry, startNanos, status, "HTTP_" + status, msg);
      throw new AiServiceException(msg);
    } catch (RestClientException e) {
      log.error("[AiService] Gemini API bağlantı hatası: {}", e.getClass().getSimpleName());
      String msg = "AI servisine şu anda ulaşılamıyor. Lütfen tekrar deneyin.";
      recordError(entry, startNanos, null, "NETWORK_ERROR", msg);
      throw new AiServiceException(msg);
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("[AiService] Beklenmeyen AI hatası: {}", e.getClass().getSimpleName());
      String msg = "Üretim sırasında beklenmeyen bir hata oluştu.";
      recordError(entry, startNanos, null, "UNEXPECTED_ERROR", msg);
      throw new AiServiceException(msg);
    }
  }

  private void recordSuccess(AiUsageLog entry, String parsedOutput) {
    if (aiUsageLogWriter == null || entry == null) {
      return;
    }
    entry.setStatus("SUCCESS");
    entry.setParsedOutput(parsedOutput);
    aiUsageLogWriter.save(entry);
  }

  private void recordParseFailure(AiUsageLog entry, String errorCode, String errorMessage) {
    if (aiUsageLogWriter == null || entry == null) {
      return;
    }
    entry.setStatus("ERROR");
    entry.setErrorCode(errorCode);
    entry.setErrorMessage(errorMessage);
    aiUsageLogWriter.save(entry);
  }

  private void recordError(
      AiUsageLog entry, long startNanos, Integer httpStatus, String errorCode, String errorMessage) {
    if (aiUsageLogWriter == null || entry == null) {
      return;
    }
    entry.setDurationMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
    entry.setHttpStatus(httpStatus);
    entry.setStatus("ERROR");
    entry.setErrorCode(errorCode);
    entry.setErrorMessage(errorMessage);
    aiUsageLogWriter.save(entry);
  }

  static void populateTokenUsage(AiUsageLog entry, JsonNode root) {
    if (entry == null || root == null) {
      return;
    }
    JsonNode usage = root.path("usage");
    if (usage.isMissingNode() || usage.isNull()) {
      return;
    }
    int input = usage.path("total_input_tokens").asInt(usage.path("promptTokenCount").asInt(0));
    int output = usage.path("total_output_tokens").asInt(usage.path("candidatesTokenCount").asInt(0));
    int thought = usage.path("total_thought_tokens").asInt(usage.path("thoughtsTokenCount").asInt(0));
    int cached = usage.path("total_cached_tokens").asInt(usage.path("cachedContentTokenCount").asInt(0));
    int total = usage.path("total_tokens").asInt(usage.path("totalTokenCount").asInt(input + output + thought));

    entry.setInputTokens(input);
    entry.setOutputTokens(output);
    entry.setThoughtTokens(thought);
    entry.setCachedTokens(cached);
    entry.setTotalTokens(total);
  }

  static String extractTextFromInteraction(JsonNode root) {
    if (root == null) {
      return null;
    }
    if (root.hasNonNull("output_text") && !root.path("output_text").asText("").isBlank()) {
      return root.path("output_text").asText();
    }
    JsonNode steps = root.path("steps");
    if (steps.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode step : steps) {
        if ("model_output".equals(step.path("type").asText())) {
          JsonNode contents = step.path("content");
          if (contents.isArray()) {
            for (JsonNode item : contents) {
              if ("text".equals(item.path("type").asText()) || item.hasNonNull("text")) {
                sb.append(item.path("text").asText(""));
              }
            }
          }
        }
      }
      if (!sb.isEmpty()) {
        return sb.toString();
      }
    }
    // Fallback for legacy candidates structure if ever returned
    JsonNode candidates = root.path("candidates");
    if (candidates.isArray() && !candidates.isEmpty()) {
      JsonNode parts = candidates.get(0).path("content").path("parts");
      if (parts.isArray() && !parts.isEmpty()) {
        return parts.get(0).path("text").asText(null);
      }
    }
    return null;
  }

  private static DtoAiUsageLog toDto(AiUsageLog entity) {
    return DtoAiUsageLog.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .requestId(entity.getRequestId())
        .stepOrder(entity.getStepOrder())
        .endpoint(entity.getEndpoint())
        .action(entity.getAction())
        .model(entity.getModel())
        .username(entity.getUsername())
        .tenantId(entity.getTenantId())
        .userInput(entity.getUserInput())
        .promptText(entity.getPromptText())
        .responseText(entity.getResponseText())
        .parsedOutput(entity.getParsedOutput())
        .promptChars(entity.getPromptChars())
        .responseChars(entity.getResponseChars())
        .inputTokens(entity.getInputTokens())
        .outputTokens(entity.getOutputTokens())
        .thoughtTokens(entity.getThoughtTokens())
        .cachedTokens(entity.getCachedTokens())
        .totalTokens(entity.getTotalTokens())
        .durationMs(entity.getDurationMs())
        .status(entity.getStatus())
        .httpStatus(entity.getHttpStatus())
        .errorCode(entity.getErrorCode())
        .errorMessage(entity.getErrorMessage())
        .build();
  }

  private static String resolveCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return null;
    }
    return auth.getName();
  }

  private String toJsonSafe(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      return obj != null ? obj.toString() : null;
    }
  }

  private static String newRequestId() {
    return UUID.randomUUID().toString();
  }

  private static String trimToNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value.trim();
  }

  private static String truncate(String text, int maxLen) {
    if (text == null || text.length() <= maxLen) {
      return text;
    }
    return text.substring(0, maxLen) + "...";
  }

  private static long toLong(Object[] row, int idx) {
    if (row == null || idx >= row.length || row[idx] == null) {
      return 0L;
    }
    if (row[idx] instanceof Number n) {
      return n.longValue();
    }
    return Long.parseLong(row[idx].toString());
  }

  private static double toDouble(Object[] row, int idx) {
    if (row == null || idx >= row.length || row[idx] == null) {
      return 0.0;
    }
    if (row[idx] instanceof Number n) {
      return n.doubleValue();
    }
    return Double.parseDouble(row[idx].toString());
  }

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new BadRequestException(message);
    }
    return value.trim();
  }

  private static String stripHtmlForPrompt(String html, int maxLen) {
    if (html == null || html.isBlank()) {
      return "";
    }
    String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    if (text.length() > maxLen) {
      return text.substring(0, maxLen);
    }
    return text;
  }

  private static String stripCodeFences(String input) {
    if (input == null) {
      return "";
    }
    return input.trim()
        .replaceFirst("(?i)^```(?:json|html)?\\s*", "")
        .replaceFirst("(?i)\\s*```$", "")
        .trim();
  }

  private String sanitizeErrorText(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String cleaned = raw.trim();
    if (!apiKey.isBlank()) {
      cleaned = cleaned.replace(apiKey, "[REDACTED_API_KEY]");
    }
    cleaned = cleaned.replaceAll("AIza[0-9A-Za-z_\\-]{20,}", "[REDACTED_API_KEY]");
    cleaned = cleaned.replaceAll("AQ\\.[0-9A-Za-z_\\-]{20,}", "[REDACTED_API_KEY]");
    return cleaned;
  }

  private String extractUpstreamErrorMessage(String sanitizedBody) {
    if (sanitizedBody == null || sanitizedBody.isBlank()) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(sanitizedBody);
      JsonNode errNode = root.isArray() && !root.isEmpty() ? root.get(0).path("error") : root.path("error");
      if (!errNode.isMissingNode() && errNode.hasNonNull("message")) {
        return truncate(errNode.path("message").asText("").trim(), 400);
      }
    } catch (Exception ignored) {
      // ignore non-json error body
    }
    return null;
  }

  public static class AiServiceException extends BaseException {
    public AiServiceException(String message) {
      super(message, HttpStatus.UNPROCESSABLE_ENTITY, "AI_GENERATION_FAILED");
    }
  }
}
