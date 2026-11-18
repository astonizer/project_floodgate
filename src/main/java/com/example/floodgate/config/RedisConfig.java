package com.example.floodgate.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis infrastructure configuration.
 *
 * <p>Spring Boot auto-configures a {@code RedisTemplate<Object, Object>} with
 * {@link org.springframework.data.redis.serializer.JdkSerializationRedisSerializer}
 * by default, which stores keys and values as binary Java-serialized bytes.
 * This is unreadable in Redis CLI, incompatible with non-Java clients, and
 * harder to debug.
 *
 * <p>This configuration overrides the default bean with a
 * {@code RedisTemplate<String, String>} that uses UTF-8 string serialization
 * for all keys and values, so Redis entries look like:
 * <pre>
 *   key:   payout_lock:PAY-001
 *   value: 550e8400-e29b-41d4-a716-446655440000  (ownership UUID)
 * </pre>
 *
 * <p>The {@link RedisConnectionFactory} is auto-configured by Spring Boot from
 * {@code spring.data.redis.host} and {@code spring.data.redis.port} in
 * {@code application.yml} using Lettuce as the underlying client.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer ser = new StringRedisSerializer();
        template.setKeySerializer(ser);
        template.setValueSerializer(ser);
        template.setHashKeySerializer(ser);
        template.setHashValueSerializer(ser);
        template.afterPropertiesSet();
        return template;
    }

}
