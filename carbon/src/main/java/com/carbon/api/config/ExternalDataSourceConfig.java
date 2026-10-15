package com.carbon.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ExternalDataSourceConfig {
    
    @Autowired
    private DataSource primaryDataSource; // 使用主數據源
    
    @Bean("externalJdbcTemplate")
    public JdbcTemplate externalJdbcTemplate() {
        return new JdbcTemplate(primaryDataSource);
    }
}
