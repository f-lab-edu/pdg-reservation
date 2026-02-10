package com.pdg.reservation.member.controller;


import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.util.JwtTokenProvider;
import com.pdg.reservation.member.dto.LoginRequest;
import com.pdg.reservation.member.dto.LoginResponse;
import com.pdg.reservation.member.dto.TokenResponse;
import com.pdg.reservation.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                     HttpServletResponse response) {
        LoginResponse loginResponse = memberService.login(loginRequest);
        jwtTokenProvider.setRefreshTokenCookie(response, loginResponse.getRefreshToken());
        return ApiResponse.ok(loginResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                        HttpServletResponse response) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.JWT_REFRESH_TOKEN_NOT_FOUND);
        }
        TokenResponse tokenResponse = memberService.reissue(refreshToken);
        jwtTokenProvider.setRefreshTokenCookie(response, tokenResponse.getRefreshToken());

        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.extractToken(request);
        memberService.logout(accessToken);
        jwtTokenProvider.deleteCookie(response);
        return  ApiResponse.message("logout success");
    }


}
