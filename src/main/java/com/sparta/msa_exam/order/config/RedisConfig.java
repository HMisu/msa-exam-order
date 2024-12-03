package com.sparta.msa_exam.order.config;

import com.sparta.msa_exam.order.dto.OrderResponseDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, OrderResponseDto> orderTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, OrderResponseDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        Jackson2JsonRedisSerializer<OrderResponseDto> serializer = new Jackson2JsonRedisSerializer<>(OrderResponseDto.class);

        template.setValueSerializer(serializer);
        template.setKeySerializer(RedisSerializer.string());
        return template;
    }
}
