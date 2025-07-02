package com.carbon.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import java.util.TimeZone;

@SpringBootApplication
@EntityScan(basePackages = "com.carbon.api.entity")
public class CarbonApplication {

    public static void main(String[] args) {
        // 设置默认时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        
        SpringApplication.run(CarbonApplication.class, args);
    }
} 