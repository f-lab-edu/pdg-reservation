package com.pdg.reservation.member.service;

import com.pdg.reservation.common.auth.repository.JwtRedisRepository;
import com.pdg.reservation.common.auth.security.CustomUserDetails;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.util.JwtTokenProvider;
import com.pdg.reservation.member.dto.LoginRequest;
import com.pdg.reservation.member.dto.LoginResponse;
import com.pdg.reservation.member.dto.TokenResponse;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.member.repository.MemberRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final JwtRedisRepository jwtRedisRepository;

    public LoginResponse login(LoginRequest loginRequest) {

        try{
            //authenticate 호출 시, 자동으로 내부에서 UserDetailsService를 구현한 Bean의 loadUserByUsername 실행 및 패스워드 비교, 유효성 검사 진행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            String email = userDetails.getEmail();
            String nickName = userDetails.getNickName();

            String accessToken = jwtTokenProvider.generateAccessToken(email);
            String refreshToken = jwtTokenProvider.generateRefreshToken(email);
            long accessTokenExpiredValue = jwtTokenProvider.getAccessTokenExpiredValue();
            jwtRedisRepository.saveRefreshToken(email, refreshToken);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiresIn(accessTokenExpiredValue)
                    .refreshToken(refreshToken)
                    .nickName(nickName)
                    .build();

        } catch (AuthenticationException e) {
            throw new CustomException(ErrorCode.AUTH_USER_NOT_FOUND, e);
        }
    }

    public TokenResponse reissue(String refreshToken) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.JWT_INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);

        String oldRefreshToken = jwtRedisRepository.findRefreshTokenByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.JWT_REFRESH_TOKEN_NOT_FOUND));

        if(!refreshToken.equals(oldRefreshToken)) {
            jwtRedisRepository.deleteRefreshTokenByEmail(email);
            log.warn("Refresh Token 불일치 감지 - 계정 보호를 위해 토큰을 삭제합니다. Email: {}", email);
            throw new CustomException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        long newAccessExpireIn = jwtTokenProvider.getAccessTokenExpiredValue();
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        jwtRedisRepository.saveRefreshToken(email, newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .accessTokenExpiresIn(newAccessExpireIn)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String accessToken) {
        String email = jwtTokenProvider.getUserEmailFromToken(accessToken);
        Long expiration = jwtTokenProvider.getTokenTTL(accessToken);
        jwtRedisRepository.deleteRefreshTokenByEmail(email);
        jwtRedisRepository.setAccessTokenBlacklist(accessToken, expiration);
    }


}
