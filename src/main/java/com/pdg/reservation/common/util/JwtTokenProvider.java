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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${accessToken.expired.value}")
    int accessTokenTokenExpiredValue;

    @Value("${refreshToken.expired.value}")
    int refreshTokenExpiredValue;

    @Value("${jwt.secret}")
    String jwtSecretStringKey;

    private final UserDetailsService userDetailsService;

    private SecretKey jwtSecretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretStringKey);
        this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
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
                    .signWith(jwtSecretKey)
                    .compact();
        } catch (JwtException e) {
            throw new JwtException("JWT 토큰 생성 중 오류가 발생했습니다.", e);
        } catch (Exception ee) {
            throw new RuntimeException("JWT 토큰 생성 중 오류가 발생했습니다.", ee);
        }
        return accessToken;
    }

    // Refresh Token 생성
    public String generateRefreshToken(String email) {
        String refreshToken;
        try {
            refreshToken = Jwts.builder()
                    .claims() // Claims 객체를 시작하는 최신 방식
                    .subject(email)
                    .expiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(refreshTokenExpiredValue)))
                    .issuedAt(new Date(System.currentTimeMillis())) // 현재 시간
                    .and()
                    .signWith(jwtSecretKey)
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

    // subject(고객 ID) 추출
    public String getUserEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Validate Token 생성
    public boolean validateToken(String token) throws JwtException {
        Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
    //RefreshToken 쿠키 추출
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    //RefreshToken 쿠키 삭제
    public void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    //Access Token의 남은 만료 시간 (TTL)을 밀리초 단위로 반환
    public Long getExpiration(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(jwtSecretKey)
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

    public Authentication getAuthentication(String token) {
        // 1. 토큰에서 사용자 ID을 가져옴 (sub)
        String email = getUserEmailFromToken(token);

        // 2. UserDetailsService를 통해 사용자 정보(권한 포함)를 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // 3. 인증 토큰 생성
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }
}
