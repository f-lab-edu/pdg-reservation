package com.pdg.reservation.common.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class JwtRedisRepository {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACK_LIST_TOKEN_PREFIX = "BL:";

    @Value("${refreshToken.expired.value}")
    int refreshTokenExpiredValue;

    // RedisTemplate<Key의 타입, Value의 타입>
    private final StringRedisTemplate redisTemplate;

    /**
     * Refresh Token을 Redis에 저장하고 만료 시간을 설정합니다.
     * @param email 사용자 식별자 (Key)
     * @param refreshToken 발급된 Refresh Token 문자열 (Value)
     */
    public void saveRefreshToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        // opsForValue(): String-String 형태의 데이터를 다루는 오퍼레이션
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshTokenExpiredValue,
                TimeUnit.SECONDS // 만료 시간 단위 설정
        );
    }

    /**
     * Redis에서 Refresh Token을 조회합니다.
     * @param email 사용자 식별자 (Key)
     * @return 저장된 Refresh Token 문자열 (Optional로 반환하여 null 체크 용이)
     */
    public Optional<String> findRefreshTokenByEmail(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    /**
     * Redis에서 Refresh Token을 삭제합니다. (로그아웃 시 사용)
     * @param email 사용자 식별자 (Key)
     */
    public void deleteRefreshTokenByEmail(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
    }

    /**
     * Access Token을 블랙리스트에 등록합니다.
     * @param accessToken 키 (블랙리스트 대상)
     * @param expirationTimeMillis Access Token의 남은 만료 시간 (밀리초)
     */
    public void setAccessTokenBlacklist(String accessToken, Long expirationTimeMillis) {
        String key = BLACK_LIST_TOKEN_PREFIX + accessToken;
        // Redis에 저장: TTL은 Access Token의 남은 유효 시간(밀리초)을 초 단위로 변환하여 설정
        redisTemplate.opsForValue().set(
                key,
                "logout",
                expirationTimeMillis, // TTL 값
                TimeUnit.MILLISECONDS // TTL 단위
        );
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인힙나다.
     * @param accessToken 키 (블랙리스트 대상)
     */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        String key = BLACK_LIST_TOKEN_PREFIX + accessToken;
        // Key가 존재하는지 확인 (Value가 무엇인지는 중요하지 않음)
        return redisTemplate.hasKey(key);
    }






}
