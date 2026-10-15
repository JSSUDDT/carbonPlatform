package com.carbon.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@EnableScheduling  // 啟用定時任務
@SpringBootApplication
@EntityScan(basePackages = "com.carbon.api.entity")
public class CarbonApplication {

    public static void main(String[] args) {
        // 設置默認時區
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        
        SpringApplication.run(CarbonApplication.class, args);
    }
} 