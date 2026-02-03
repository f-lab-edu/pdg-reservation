package com.pdg.reservation.common.config;

import com.pdg.reservation.common.interceptor.CommonInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CommonInterceptor commonInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonInterceptor).addPathPatterns("/**");
    }
}
