package com.cms.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class RedisCacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    ObjectMapper objectMapper = createObjectMapper();
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10)) // 10 dakika TTL
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
        .disableCachingNullValues();

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(cacheConfiguration)
        .transactionAware()
        .build();
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    // JavaTimeModule for LocalDateTime support
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Hibernate6Module - Handle lazy-loading proxies
    Hibernate6Module hibernate6Module = new Hibernate6Module();
    // Don't force lazy loading - skip if not initialized
    hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
    // Don't serialize identifier for lazy not loaded objects
    hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);
    // Write missing entities as null
    hibernate6Module.configure(Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL, true);
    // CRITICAL: Replace Hibernate collections (PersistentSet) with regular Java
    // collections
    hibernate6Module.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);
    objectMapper.registerModule(hibernate6Module);

    // Don't fail on empty beans (Hibernate proxies)
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // Ignore unknown properties during deserialization
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Type validator that allows common types but excludes Hibernate internal types
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Object.class)
        .build();

    // Enable type info for polymorphic deserialization
    objectMapper.activateDefaultTyping(
        ptv,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);

    return objectMapper;
  }
}
