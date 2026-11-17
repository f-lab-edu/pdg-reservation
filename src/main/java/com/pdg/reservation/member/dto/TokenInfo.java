package com.pdg.reservation.member.dto;

// 토큰 재발급 결과. 리프레시 토큰은 Controller가 쿠키로 내려준다
public record TokenInfo(
        String accessToken,
        Long accessTokenExpiresIn,
        String refreshToken) {
}
