package com.pdg.reservation.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    @Builder.Default
    private String authType = "Bearer";
    private String accessToken;
    private Long accessTokenExpiresIn;
    @JsonIgnore
    private String refreshToken;

}
