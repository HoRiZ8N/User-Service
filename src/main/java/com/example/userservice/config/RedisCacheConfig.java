package com.example.userservice.config;

import com.example.userservice.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class RedisCacheConfig {

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerCustomizer(
      ObjectMapper objectMapper,
      @Value("${app.cache.users-ttl}") Duration usersTtl) {
    ObjectMapper cacheMapper = objectMapper.copy()
        .setAnnotationIntrospector(new IgnoreAccessIntrospector());
    RedisCacheConfiguration usersConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(usersTtl)
        .disableCachingNullValues()
        .serializeValuesWith(SerializationPair.fromSerializer(
            new Jackson2JsonRedisSerializer<>(cacheMapper, UserDto.class)));
    return builder -> builder
        .transactionAware()
        .withCacheConfiguration(CacheNames.USERS, usersConfig);
  }

  private static class IgnoreAccessIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public JsonProperty.Access findPropertyAccess(Annotated annotated) {
      return JsonProperty.Access.READ_WRITE;
    }
  }
}
