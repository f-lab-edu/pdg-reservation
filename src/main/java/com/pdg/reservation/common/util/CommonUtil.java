package com.pdg.reservation.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class CommonUtil {

    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if(ip == null || ip.isEmpty()) ip = request.getHeader("Proxy-Client-IP");
        if(ip == null || ip.isEmpty()) ip = request.getHeader("WL-Proxy-Client-IP");
        if(ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if(ip.contains(",")) ip = ip.substring(ip.indexOf(",")+1);
        return ip;
    }

    /**
     * 닉네임 마스킹 처리
     * 2글자: 홍길 -> 홍*
     * 3글자: 홍길동 -> 홍*동
     * 4글자 이상: 남궁민수 -> 남**수
     */
    public static String maskNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "";
        }

        int length = nickname.length();

        if (length <= 1) {
            return "*";
        }

        if (length == 2) {
            return nickname.substring(0, 1) + "*";
        }

        // 중간 글자들을 모두 *로 치환 (첫 글자와 마지막 글자만 노출)
        String middleMask = "*".repeat(length - 2);
        return nickname.substring(0, 1) + middleMask + nickname.substring(length - 1);
    }

}
