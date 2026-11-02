package com.pdg.reservation.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenResponse {

    @Builder.Default
    private String authType = "Bearer";
    private String accessToken;
    private Long accessTokenExpiresIn;
    @JsonIgnore
    private String refreshToken;

}
