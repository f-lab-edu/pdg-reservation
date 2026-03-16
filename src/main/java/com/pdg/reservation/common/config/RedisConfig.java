package com.pdg.reservation.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pdg.reservation.common.constant.RedisKeyNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Configuration
@EnableCaching // 캐시 기능 활성화
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화 (모든 자료구조의 최상위 Key)
        template.setKeySerializer(new StringRedisSerializer());

        // Value 직렬화 (String, List, Set 등의 값)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Hash Key 직렬화 (Hash 내부의 필드 이름)
        template.setHashKeySerializer(new StringRedisSerializer());

        // Hash Value 직렬화 (Hash 내부 필드의 값, 객체 저장을 위해 JSON 사용)
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. Java 8 날짜/시간을 지원하는 ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalTime, LocalDateTime 지원
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 형식으로 저장

        // [다형성 타입 정보 활성화 - 핵심 설정]
        // JSON 데이터에 "@class" 필드를 추가하여 역직렬화 시 어떤 객체로 변환할지 명시합니다.
        // 이 설정이 없으면 Redis에서 데이터를 읽을 때 LinkedHashMap으로 캐스팅되는 에러가 발생합니다.
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // GenericJackson2JsonRedisSerializer에 설정된 ObjectMapper 주입
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 2. 기본 설정 (Default)
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(createDynamicJitter(30)); // 기본 30분

        // 3. 캐시 이름별 차등 설정 (숙소 상세와 리뷰 목록의 수명이 다를 수 있음)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 숙소 상세 정보: 변경이 적으므로 1시간 동안 유지
        cacheConfigurations.put(RedisKeyNames.ACCOMMODATION_DETAIL,
                defaultCacheConfig.entryTtl(createDynamicJitter(60)));

        // 리뷰 첫 페이지: 최신 리뷰가 자주 올라오므로 10분으로 짧게 유지
        cacheConfigurations.put(RedisKeyNames.REVIEW_FIRST_PAGE,
                defaultCacheConfig.entryTtl(createDynamicJitter(10)));

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(connectionFactory)
                .cacheDefaults(defaultCacheConfig) // 기본값 적용
                .withInitialCacheConfigurations(cacheConfigurations) // 개별 설정 적용
                .build();
    }

    /**
     * 캐시 아발란체 방어
     * 매 호출 시점마다 새로운 Duration을 계산하여 반환하는 TtlFunction 생성
     * 0초 ~ 300초(5분) 사이에서 초 단위로 무작위 분산
     */
    private RedisCacheWriter.TtlFunction createDynamicJitter(int baseMinutes) {
        return (key, value) -> {
            log.info("캐시 jitter 난수 생성기 실행 key : {},  value : {}", key, value);

            // [핵심 로직] 데이터가 null(존재하지 않음)이면 5분만 저장하여 관통 방어
            if (value == null) {
                log.info("캐시 관통 방어: Key {}에 대해 Null 캐싱(5분) 적용", key);
                return Duration.ofMinutes(5);
            }

            int jitter = ThreadLocalRandom.current().nextInt(301); // 0~4분 랜덤 추가
            return Duration.ofMinutes(baseMinutes).plusSeconds(jitter);
        };
    }

}
