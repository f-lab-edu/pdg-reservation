package com.pdg.reservation.fixture;

// 로그인 한 번으로 받은 두 토큰
public record LoginSession(String accessToken, String refreshToken) {

    public String bearer() {
        return "Bearer " + accessToken;
    }
}
