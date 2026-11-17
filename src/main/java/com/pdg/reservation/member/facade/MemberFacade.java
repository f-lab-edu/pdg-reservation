package com.pdg.reservation.member.facade;

import com.pdg.reservation.member.dto.LoginRequest;
import com.pdg.reservation.member.dto.LoginInfo;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.member.dto.TokenInfo;
import com.pdg.reservation.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberFacade {

    private final MemberService memberService;

    @Transactional
    public void signup(SignupRequest request) {
        memberService.signup(request);
    }

    public LoginInfo login(LoginRequest request) {
        return memberService.login(request);
    }

    public TokenInfo reissue(String refreshToken) {
        return memberService.reissue(refreshToken);
    }

    public void logout(String accessToken) {
        memberService.logout(accessToken);
    }
}
