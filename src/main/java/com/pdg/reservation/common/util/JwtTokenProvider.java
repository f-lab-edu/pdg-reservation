package com.pdg.reservation.common.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// 액세스 토큰과 리프레시 토큰은 서로 다른 키로 서명한다(ADR 0019).
// 리프레시 토큰이 액세스 토큰 자리에서 통과해 로그아웃과 Rotation을 우회하지 못하게 서명 검증 단계에서 구분한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${accessToken.expired.value}")
    int accessTokenTokenExpiredValue;

    @Value("${refreshToken.expired.value}")
    int refreshTokenExpiredValue;

    @Value("${jwt.access-secret}")
    String jwtAccessSecretStringKey;

    @Value("${jwt.refresh-secret}")
    String jwtRefreshSecretStringKey;

    private final UserDetailsService userDetailsService;

    private SecretKey jwtAccessKey;
    private SecretKey jwtRefreshKey;

    @PostConstruct
    public void init() {
        this.jwtAccessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecretStringKey));
        this.jwtRefreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecretStringKey));
    }

    // Access Token 생성
    public String generateAccessToken(String email) {
        String accessToken;
        try {
            accessToken = Jwts.builder()
                    .claims() // Claims 객체를 시작하는 최신 방식
                    .subject(email)
                    .expiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(accessTokenTokenExpiredValue)))
                    .issuedAt(new Date(System.currentTimeMillis())) // 현재 시간
                    .and()
                    .signWith(jwtAccessKey)
                    .compact();
        } catch (JwtException e) {
            throw new JwtException("JWT 토큰 생성 중 오류가 발생했습니다.", e);
        } catch (Exception ee) {
            throw new RuntimeException("JWT 토큰 생성 중 오류가 발생했습니다.", ee);
        }
        return accessToken;
    }

    //엑세스 토큰 사용 시간
    public long getAccessTokenExpiredValue() {
        return this.accessTokenTokenExpiredValue;
    }

    // Refresh Token 생성
    public String generateRefreshToken(String email) {
        String refreshToken;
        try {
            refreshToken = Jwts.builder()
                    .claims() // Claims 객체를 시작하는 최신 방식
                    // 토큰마다 고유해야 한다. exp/iat는 초 단위라 같은 초에 발급하면 토큰이 같아지고,
                    // 그러면 재발급 후에도 이전 토큰이 Redis 값과 일치해 Rotation이 무력화된다.
                    .id(UUID.randomUUID().toString())
                    .subject(email)
                    .expiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(refreshTokenExpiredValue)))
                    .issuedAt(new Date(System.currentTimeMillis())) // 현재 시간
                    .and()
                    .signWith(jwtRefreshKey)
                    .compact();
        } catch (JwtException e) {
            throw new JwtException("JWT 토큰 생성 중 오류가 발생했습니다.", e);
        } catch (Exception ee) {
            throw new RuntimeException("JWT 토큰 생성 중 오류가 발생했습니다.", ee);
        }
        return refreshToken;
    }

    // Authorization 헤더에서 JWT 토큰 추출
    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 접두사 제거
        }
        return null; // 헤더가 없거나 Bearer로 시작하지 않으면 null 반환
    }

    // 액세스 토큰의 subject(이메일) 추출. access 키로 검증한다
    public String getEmailFromAccessToken(String token) {
        return parseSubject(token, jwtAccessKey);
    }

    // 리프레시 토큰의 subject(이메일) 추출. refresh 키로 검증한다
    public String getEmailFromRefreshToken(String token) {
        return parseSubject(token, jwtRefreshKey);
    }

    // 액세스 토큰인지 검증한다. 리프레시 토큰은 다른 키로 서명되어 여기서 거절된다
    public boolean validateAccessToken(String token) {
        return validate(token, jwtAccessKey);
    }

    // 리프레시 토큰인지 검증한다. 액세스 토큰은 다른 키로 서명되어 여기서 거절된다
    public boolean validateRefreshToken(String token) {
        return validate(token, jwtRefreshKey);
    }

    private String parseSubject(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private boolean validate(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // RefreshToken Cookie 발급
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
        cookie.setMaxAge(60 * refreshTokenExpiredValue);
        response.addCookie(cookie);
    }

    //RefreshToken 쿠키 삭제
    public void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    //Access Token의 남은 만료 시간 (TTL)을 밀리초 단위로 반환
    public Long getAccessTokenTTL(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(jwtAccessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();

            // 현재 시각과 만료 시각의 차이 계산 (밀리초)
            long now = new Date().getTime();
            // 남은 시간 = 만료 시간(밀리초) - 현재 시간(밀리초)
            return expiration.getTime() - now;
        } catch (Exception e) {
            // 토큰이 유효하지 않거나 이미 만료된 경우 (남은 시간이 0 이하가 됨)
            return 0L;
        }
    }

    // 액세스 토큰으로 인증 객체를 만든다
    public Authentication getAuthentication(String accessToken) {
        // 1. 토큰에서 사용자 ID을 가져옴 (sub)
        String email = getEmailFromAccessToken(accessToken);

        // 2. UserDetailsService를 통해 사용자 정보(권한 포함)를 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // 3. 인증 토큰 생성
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }
}
