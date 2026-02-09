package com.pdg.reservation.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    @Builder.Default
    private String authType = "Bearer";
    private String accessToken;             // 엑세스 토큰
    private Long accessTokenExpiresIn;      // 엑세스 토큰 만료시간

    @JsonIgnore
    private String refreshToken;            // 리프레시 토큰
    private String nickName;                // "박대광" (화면 표시용)


}
