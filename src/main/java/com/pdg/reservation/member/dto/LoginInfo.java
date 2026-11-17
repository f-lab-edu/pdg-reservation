package com.pdg.reservation.member.dto;

// 로그인 결과. 리프레시 토큰은 Controller가 쿠키로 내려준다
public record LoginInfo(
        String accessToken,
        Long accessTokenExpiresIn,
        String refreshToken,
        String nickName) {
}
