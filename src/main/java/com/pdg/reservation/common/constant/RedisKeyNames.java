package com.pdg.reservation.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisKeyNames {

    // 캐시 네임스페이스 (RedisConfig와 @Cacheable에서 공유)
    // 스프링이 관리하는 캐시는 ::가 관례적으로 붙음
    public static final String REVIEW_FIRST_PAGE = "review:firstpage";
    public static final String ACCOMMODATION_DETAIL = "accommodation:detail";

    // 직접 사용하는 키 접두사 (RedisTemplate용)
    // 직접 관리하는 캐시를 알아보기 쉽게 하기 위해 : 하나만 붙혀서 구분
    public static final String REVIEW_VERSION_PREFIX = "review:version:";


}
