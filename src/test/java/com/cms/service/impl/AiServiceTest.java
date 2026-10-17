package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
import com.cms.dto.DtoAiUsageStats;
import com.cms.entity.AiUsageLog;
import com.cms.exception.BadRequestException;
import com.cms.repository.AiUsageLogRepository;
import com.cms.service.impl.AiService.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiServiceTest {

  private static final String INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1/interactions";
  private static final String TEST_API_KEY = "test-secret-api-key-123";

  private ObjectMapper objectMapper;
  private RestClient.Builder builder;
  private MockRestServiceServer mockServer;
  private AiUsageLogRepository logRepository;
  private AiUsageLogWriter logWriter;
  private AiService aiService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    logRepository = mock(AiUsageLogRepository.class);
    logWriter = mock(AiUsageLogWriter.class);
    when(logWriter.save(any())).thenAnswer(inv -> inv.getArgument(0));
    aiService = new AiService(
        objectMapper, builder.build(), TEST_API_KEY, "gemini-3.8-flash", logRepository, logWriter);
  }

  private String interactionJson(String text) {
    return """
        {
          "id": "v1_test",
          "model": "gemini-3.8-flash",
          "object": "interaction",
          "status": "completed",
          "usage": {
            "total_input_tokens": 42,
            "total_output_tokens": 18,
            "total_thought_tokens": 120,
            "total_cached_tokens": 5,
            "total_tokens": 180
          },
          "steps": [
            {
              "type": "model_output",
              "content": [
                {
                  "type": "text",
                  "text": %s
                }
              ]
            }
          ]
        }
        """.formatted(text);
  }

  @Test
  void generateSlug_normalizesTurkishAndSpecialCharsAndRecordsUsageLog() {
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-goog-api-key", TEST_API_KEY))
        .andRespond(withSuccess(interactionJson("\"  Spring Boot 3 -- İle Çoklu Kiracı!  \""), MediaType.APPLICATION_JSON));

    DtoAiSlugResponse res = aiService.generateSlug(new DtoAiTitleRequest("Spring Boot 3 ile Çoklu Kiracı"));
    assertEquals("spring-boot-3-ile-coklu-kiraci", res.getSlug());
    mockServer.verify();

    ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
    verify(logWriter).save(captor.capture());
    AiUsageLog saved = captor.getValue();
    assertEquals("/api/v1/ai/slug", saved.getEndpoint());
    assertEquals("slug", saved.getAction());
    assertEquals(1, saved.getStepOrder());
    assertEquals("SUCCESS", saved.getStatus());
    assertEquals(42, saved.getInputTokens());
    assertEquals(18, saved.getOutputTokens());
    assertEquals(120, saved.getThoughtTokens());
    assertEquals(5, saved.getCachedTokens());
    assertEquals(180, saved.getTotalTokens());
    assertTrue(saved.getParsedOutput().contains("spring-boot-3-ile-coklu-kiraci"));
  }

  @Test
  void generateSeoFields_stripsMarkdownCodeFenceAndParsesJson() {
    String modelText = "\"```json\\n{\\\"seoTitle\\\":\\\"Test Başlık\\\",\\\"seoDescription\\\":\\\"Test açıklama\\\",\\\"seoKeywords\\\":\\\"java, spring\\\"}\\n```\"";
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson(modelText), MediaType.APPLICATION_JSON));

    DtoAiSeoResponse res = aiService.generateSeoFields(new DtoAiTitleRequest("Test"));
    assertEquals("Test Başlık", res.getSeoTitle());
    assertEquals("Test açıklama", res.getSeoDescription());
    assertEquals("java, spring", res.getSeoKeywords());
  }

  @Test
  void generateDescription_returnsTrimmedDescription() {
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson("\" Kısa bileşen açıklaması. \""), MediaType.APPLICATION_JSON));

    DtoAiDescriptionResponse res = aiService.generateDescription(new DtoAiDescriptionRequest("Hero", "component"));
    assertEquals("Kısa bileşen açıklaması.", res.getDescription());
  }

  @Test
  void generateAltText_returnsTrimmedAltText() {
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson("\"Kampanya afişi tanıtım görseli\""), MediaType.APPLICATION_JSON));

    DtoAiAltTextResponse res = aiService.generateAltText(new DtoAiTitleRequest("Kampanya"));
    assertEquals("Kampanya afişi tanıtım görseli", res.getAltText());
  }

  @Test
  void generateBlogInfo_stripsHtmlAndParsesJson() {
    String modelText = "\"{\\\"description\\\":\\\"Özet\\\",\\\"category\\\":\\\"Backend\\\",\\\"readingTime\\\":\\\"4 dk okuma\\\",\\\"imagePrompt\\\":\\\"modern cloud server\\\"}\"";
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson(modelText), MediaType.APPLICATION_JSON));

    DtoAiBlogInfoResponse res = aiService.generateBlogInfo(
        new DtoAiBlogInfoRequest("Başlık", "<p>Merhaba <strong>dünya</strong> içerik</p>"));
    assertEquals("Özet", res.getDescription());
    assertEquals("Backend", res.getCategory());
    assertEquals("4 dk okuma", res.getReadingTime());
    assertEquals("modern cloud server", res.getImagePrompt());
  }

  @Test
  void generateBlogInfo_emptyPlainContent_throwsBadRequest() {
    assertThrows(BadRequestException.class,
        () -> aiService.generateBlogInfo(new DtoAiBlogInfoRequest("Başlık", "<p>   </p>")));
  }

  @Test
  void generateArticle_runsThreeStepPipelineAndLogsThreeCorrelatedRows() {
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson("\"1. Giriş\\n2. Mimari\""), MediaType.APPLICATION_JSON));
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson("\"<h1>Başlık</h1><p>Ham içerik</p>\""), MediaType.APPLICATION_JSON));
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withSuccess(interactionJson("\"```html\\n<h1>Başlık</h1><p>Temiz içerik</p>\\n```\""), MediaType.APPLICATION_JSON));

    DtoAiArticleResponse res = aiService.generateArticle(new DtoAiArticleRequest("Spring Boot Mimari", "java, spring"));
    assertEquals("<h1>Başlık</h1><p>Temiz içerik</p>", res.getHtml());
    mockServer.verify();

    ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
    verify(logWriter, times(3)).save(captor.capture());
    List<AiUsageLog> rows = captor.getAllValues();
    assertEquals("article:outline", rows.get(0).getAction());
    assertEquals(1, rows.get(0).getStepOrder());
    assertEquals("article:draft", rows.get(1).getAction());
    assertEquals(2, rows.get(1).getStepOrder());
    assertEquals("article:sanitize", rows.get(2).getAction());
    assertEquals(3, rows.get(2).getStepOrder());
    assertEquals(rows.get(0).getRequestId(), rows.get(1).getRequestId());
    assertEquals(rows.get(1).getRequestId(), rows.get(2).getRequestId());
  }

  @Test
  void missingApiKey_throwsSanitizedMessageWithoutLeakingKey() {
    AiService noKeyService = new AiService(objectMapper, builder.build(), "", "gemini-3.8-flash", logRepository, logWriter);
    AiServiceException ex = assertThrows(AiServiceException.class,
        () -> noKeyService.generateSlug(new DtoAiTitleRequest("Test")));
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().contains("Gemini API anahtarı"));

    ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
    verify(logWriter).save(captor.capture());
    assertEquals("ERROR", captor.getValue().getStatus());
    assertEquals("API_KEY_MISSING", captor.getValue().getErrorCode());
  }

  @Test
  void invalidApiKeyHttpError_neverLeaksSecretKeyAndRecordsErrorLog() {
    mockServer.expect(requestTo(INTERACTIONS_URL))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"error\":{\"message\":\"API_KEY_INVALID: key=" + TEST_API_KEY + "\"}}"));

    AiServiceException ex = assertThrows(AiServiceException.class,
        () -> aiService.generateSlug(new DtoAiTitleRequest("Test")));
    assertTrue(!ex.getMessage().contains(TEST_API_KEY));
    assertTrue(ex.getMessage().contains("Gemini API anahtarı geçersiz"));

    ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
    verify(logWriter).save(captor.capture());
    assertEquals("ERROR", captor.getValue().getStatus());
    assertEquals("INVALID_API_KEY", captor.getValue().getErrorCode());
    assertEquals(400, captor.getValue().getHttpStatus());
  }

  @Test
  void getUsageStats_mapsRepositoryAggregates() {
    when(logRepository.aggregateTotals())
        .thenReturn(List.<Object[]>of(new Object[] {10L, 9L, 1L, 1200L, 600L, 2400L, 100L, 4200L, 845.67}));
    when(logRepository.aggregateByAction())
        .thenReturn(List.<Object[]>of(
            new Object[] {"slug", "/api/v1/ai/slug", 5L, 5L, 0L, 300L, 100L, 500L, 900L, 412.3}));

    DtoAiUsageStats stats = aiService.getUsageStats();
    assertEquals(10L, stats.getTotalCalls());
    assertEquals(9L, stats.getSuccessCalls());
    assertEquals(1L, stats.getErrorCalls());
    assertEquals(1200L, stats.getTotalInputTokens());
    assertEquals(600L, stats.getTotalOutputTokens());
    assertEquals(2400L, stats.getTotalThoughtTokens());
    assertEquals(4200L, stats.getTotalTokens());
    assertEquals(845.7, stats.getAvgDurationMs());
    assertEquals(1, stats.getByAction().size());
    assertEquals("slug", stats.getByAction().get(0).getAction());
    assertEquals(900L, stats.getByAction().get(0).getTotalTokens());
  }
}
