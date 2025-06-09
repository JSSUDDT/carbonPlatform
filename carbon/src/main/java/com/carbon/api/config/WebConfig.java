package com.carbon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 設置默認的區域解析器，使用基於Cookie的區域解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        // 設置默認語言為英文
        resolver.setDefaultLocale(Locale.ENGLISH);
        // 設置Cookie的有效期為一年
        resolver.setCookieMaxAge(60 * 60 * 24 * 365);
        // 設置Cookie名稱
        resolver.setCookieName("carbonLocale");
        return resolver;
    }

    /**
     * 設置語言切換的攔截器，設置語言變更的URL參數名稱為"lang"
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * 註冊語言切換的攔截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
} 