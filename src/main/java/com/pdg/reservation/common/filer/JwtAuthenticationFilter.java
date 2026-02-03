package com.pdg.reservation.common.filer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.reservation.common.auth.repository.JwtRedisRepository;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.util.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRedisRepository jwtRedisRepository;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtTokenProvider.extractToken(request);
        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                if (jwtRedisRepository.isAccessTokenBlacklisted(token)) {
                    throw new JwtException("블랙 리스트에 등록된 토큰입니다.");
                }
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put("email", jwtTokenProvider.getUserEmailFromToken(token));
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.info("JWT Expired : {}", e.getMessage());
            responseWriter(response, ErrorCode.JWT_EXPIRED);
        } catch (JwtException e) {
            log.info("JWT Err : {}", e.getMessage());
            responseWriter(response, ErrorCode.JWT_ERROR);
        } catch (BadCredentialsException e) {
            log.info("JWT BL : {}", e.getMessage());
            responseWriter(response, ErrorCode.AUTH_UNAUTHORIZED);
        } finally {
            MDC.clear();
        }
    }

    private void responseWriter(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(errorCode)));
    }
}
