package com.cms.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

  abstract static class ComponentMixIn {
    @JsonIgnore
    abstract java.util.Set<com.cms.entity.Page> getPages();
  }

  abstract static class CommentMixIn {
    @JsonIgnore
    abstract com.cms.entity.Comment getParentComment();
  }

  private ObjectMapper createRedisObjectMapper() {
    Hibernate6Module hibernateModule = new Hibernate6Module();
    hibernateModule.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
    hibernateModule.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);
    hibernateModule.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(hibernateModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.addMixIn(com.cms.entity.Component.class, ComponentMixIn.class);
    mapper.addMixIn(com.cms.entity.Comment.class, CommentMixIn.class);
    mapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .build(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);
    return mapper;
  }

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration() {
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
        createRedisObjectMapper());

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .computePrefixWith(cacheName -> {
          String tenantId = TenantContext.getTenantId();
          if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
          }
          return tenantId + "::" + cacheName + "::";
        })
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
      RedisCacheConfiguration redisCacheConfiguration) {
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(redisCacheConfiguration)
        .transactionAware()
        .build();
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
        createRedisObjectMapper());
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);
    return template;
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new GracefulCacheErrorHandler();
  }
}
