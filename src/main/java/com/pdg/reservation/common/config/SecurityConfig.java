package com.pdg.reservation.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.reservation.common.auth.repository.JwtRedisRepository;
import com.pdg.reservation.common.exception.filterException.CustomAccessDeniedHandler;
import com.pdg.reservation.common.exception.filterException.CustomAuthenticationEntryPoint;
import com.pdg.reservation.common.filer.JwtAuthenticationFilter;
import com.pdg.reservation.common.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        //비 로그인 접근 가능 URL
        String[] permitAllUrls = {
                "/",
                "/api/v1/members/login",
                "/api/v1/members/reissue",
        };

        //어드민 접근 가능 URL
        String[] adminPermitUrls = {};

        http
                // 모든 요청을 허용 (로그인 페이지로 가지 않음)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(permitAllUrls).permitAll()
                        .requestMatchers(adminPermitUrls).hasRole("ADMIN")
                        .anyRequest().authenticated()// 인증된 사용자만 허용
                )
                // exceptionHandling는 인증, 인가만 검사함, 컨트롤러가기전이라 GlobalExceptionHandler 예외로 빠지지 못하는 예외 처리
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint) //인증 : 401
                        .accessDeniedHandler(accessDeniedHandler)           //인가 : 403
                )
                //FORM 로그인 인증 기반 비활성화(JWT를 사용 할 것임으로 비활성화)
                .formLogin(AbstractHttpConfigurer::disable)
                //FORM 로그아웃 비활성화(JWT를 사용 할 것임으로 비활성화)
                .logout(AbstractHttpConfigurer::disable)
                //HTTP 로그인 인증 기반 비활성화(JWT를 사용 할 것임으로 비활성화)
                .httpBasic(AbstractHttpConfigurer::disable)
                //CSRF 비활성화 (JWT를 사용 할 것임으로 비활성화)
                .csrf(AbstractHttpConfigurer::disable)
                //시큐리티 세션 비활성화(JWT를 사용 할 것임으로 비활성화)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtRedisRepository jwtRedisRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, jwtRedisRepository);
    }






}