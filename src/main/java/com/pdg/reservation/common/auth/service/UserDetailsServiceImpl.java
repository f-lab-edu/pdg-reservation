package com.pdg.reservation.common.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    //private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        //User user = userJpaRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));

        List<SimpleGrantedAuthority> authorities = null;
        authorities = List.of(
                new SimpleGrantedAuthority("ROLE_NORMAL")
        );

        /*
        return new User(
                user.getEmail(),
                user.getPassword(),     // 실제로는 암호화된 비밀번호를 사용해야 합니다.
                authorities             // 사용자의 권한(ROLE) 목록
        );
        */

        //추후 엔티티 생성 후 연결 예정
        return new User(
                "iieorhkd@naver.com",
                "12341234",     // 실제로는 암호화된 비밀번호를 사용해야 합니다.
                authorities             // 사용자의 권한(ROLE) 목록
        );
    }

}
