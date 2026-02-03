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
}
