package com.sparta.msa_exam.order.config;

import com.sparta.msa_exam.order.dto.OrderResponseDto;
import com.sparta.msa_exam.order.dto.OrderSearchDto;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<OrderResponseDto> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(OrderResponseDto.class);

        RedisCacheConfiguration configuration = RedisCacheConfiguration
                .defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(60))
                .computePrefixWith(CacheKeyPrefix.simple())
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));

        return RedisCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(configuration)
                .build();
    }

    @Bean
    public KeyGenerator customCacheKeyGenerator() {
        return getKeyGenerator();
    }

    static KeyGenerator getKeyGenerator() {
        return (target, method, params) -> {
            OrderSearchDto searchDto = params.length > 0 && params[0] instanceof OrderSearchDto ? (OrderSearchDto) params[0] : new OrderSearchDto();
            Pageable pageable = params.length > 1 && params[1] instanceof Pageable ? (Pageable) params[1] : Pageable.unpaged();

            StringBuilder sb = new StringBuilder();

            if (searchDto.getStatus() != null) {
                sb.append("status_").append(searchDto.getStatus());
            }

            if (searchDto.getOrderItemIds() != null && !searchDto.getOrderItemIds().isEmpty()) {
                String orderItemIdsStr = searchDto.getOrderItemIds().stream()
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                sb.append("orderItemIds_").append(orderItemIdsStr).append("_");
            }

            sb.append("pageNum").append(pageable.getPageNumber());
            sb.append("pageSize").append(pageable.getPageSize());

            return sb.toString();
        };
    }
}
