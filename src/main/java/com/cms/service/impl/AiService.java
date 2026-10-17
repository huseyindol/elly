package com.cms.service.impl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
import com.cms.exception.BadRequestException;
import com.cms.exception.BaseException;
import com.cms.service.IAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini Interactions API (/v1/interactions) üzerinden AI içerik üretim servisi.
 * Model varsayılan olarak {@code gemini-3.8-flash} kullanır.
 * API anahtarı sadece backend'de tutulur ve hata mesajlarında asla dışarı sızdırılmaz.
 */
@Service
@Slf4j
public class AiService implements IAiService {

  private static final String INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1/interactions";
  private static final int MAX_PLAIN_CONTENT_LEN = 4000;

  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String apiKey;
  private final String modelName;

  @Autowired
  public AiService(
      ObjectMapper objectMapper,
      @Value("${app.ai.gemini.api-key:}") String apiKey,
      @Value("${app.ai.gemini.model:gemini-3.8-flash}") String modelName,
      @Value("${app.ai.gemini.connect-timeout-ms:10000}") int connectTimeoutMs,
      @Value("${app.ai.gemini.read-timeout-ms:90000}") int readTimeoutMs) {
    this(objectMapper, createDefaultRestClient(connectTimeoutMs, readTimeoutMs), apiKey, modelName);
  }

  public AiService(
      ObjectMapper objectMapper,
      RestClient restClient,
      String apiKey,
      String modelName) {
    this.objectMapper = objectMapper;
    this.restClient = restClient;
    this.apiKey = apiKey != null ? apiKey.trim() : "";
    this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "gemini-3.8-flash";
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

    String raw = callGemini(prompt);
    String slug = raw.trim()
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
      throw new AiServiceException("AI geçerli bir slug üretemedi.");
    }

    return DtoAiSlugResponse.builder().slug(slug).build();
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

    String raw = stripCodeFences(callGemini(prompt));
    try {
      JsonNode node = objectMapper.readTree(raw);
      return DtoAiSeoResponse.builder()
          .seoTitle(node.path("seoTitle").asText("").trim())
          .seoDescription(node.path("seoDescription").asText("").trim())
          .seoKeywords(node.path("seoKeywords").asText("").trim())
          .build();
    } catch (Exception e) {
      log.error("[AiService] SEO JSON ayrıştırma hatası", e);
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

    String description = callGemini(prompt).trim();
    return DtoAiDescriptionResponse.builder().description(description).build();
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

    String altText = callGemini(prompt).trim();
    return DtoAiAltTextResponse.builder().altText(altText).build();
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

    String raw = stripCodeFences(callGemini(prompt));
    try {
      JsonNode node = objectMapper.readTree(raw);
      return DtoAiBlogInfoResponse.builder()
          .description(node.path("description").asText("").trim())
          .category(node.path("category").asText("").trim())
          .readingTime(node.path("readingTime").asText("").trim())
          .imagePrompt(node.path("imagePrompt").asText("").trim())
          .build();
    } catch (Exception e) {
      log.error("[AiService] BlogInfo JSON ayrıştırma hatası", e);
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

    String outline = callGemini(outlinePrompt);
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

    String rawHtml = callGemini(articlePrompt);
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

    String cleanHtml = stripCodeFences(callGemini(reviewPrompt));
    log.info("[AiService] Makale üretimi tamamlandı");
    return DtoAiArticleResponse.builder().html(cleanHtml).build();
  }

  // ==================== INTERNAL HELPERS ====================

  private String callGemini(String prompt) {
    if (apiKey.isBlank()) {
      throw new AiServiceException(
          "Gemini API anahtarı yapılandırılmamış. Lütfen sistem yöneticisiyle iletişime geçin.");
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", modelName);
    body.put("input", prompt);
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

      if (rawResponse == null || rawResponse.isBlank()) {
        throw new AiServiceException("AI servisinden boş yanıt döndü.");
      }

      JsonNode root = objectMapper.readTree(rawResponse);
      String extracted = extractTextFromInteraction(root);
      if (extracted == null || extracted.isBlank()) {
        throw new AiServiceException("AI servisinden metin çıktısı alınamadı.");
      }
      return extracted;

    } catch (RestClientResponseException e) {
      int status = e.getStatusCode().value();
      log.error("[AiService] Gemini API HTTP hatası: status={}", status);
      if (status == 400 || status == 401 || status == 403) {
        String bodyStr = e.getResponseBodyAsString();
        if (bodyStr != null && (bodyStr.contains("API_KEY") || bodyStr.contains("api key") || status == 401 || status == 403)) {
          throw new AiServiceException(
              "Gemini API anahtarı geçersiz veya yetkisiz. Lütfen sistem yöneticisiyle iletişime geçin.");
        }
      }
      if (status == 429) {
        throw new AiServiceException("AI istek kotası aşıldı. Lütfen biraz bekleyip tekrar deneyin.");
      }
      throw new AiServiceException("AI üretimi sırasında bir hata oluştu (HTTP " + status + ").");
    } catch (RestClientException e) {
      log.error("[AiService] Gemini API bağlantı hatası: {}", e.getClass().getSimpleName());
      throw new AiServiceException("AI servisine şu anda ulaşılamıyor. Lütfen tekrar deneyin.");
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("[AiService] Beklenmeyen AI hatası: {}", e.getClass().getSimpleName());
      throw new AiServiceException("Üretim sırasında beklenmeyen bir hata oluştu.");
    }
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

  public static class AiServiceException extends BaseException {
    public AiServiceException(String message) {
      super(message, HttpStatus.BAD_GATEWAY, "AI_GENERATION_FAILED");
    }
  }
}
