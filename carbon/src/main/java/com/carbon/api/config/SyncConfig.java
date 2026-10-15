package com.carbon.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncConfig {
    private boolean enabled = true;
    private String cron = "0 */5 * * * ?";  // 每5分鐘執行一次
    private int batchSize = 1000;
    private String sourceSchema = "ccms";
    private String sourceTable = "trans_mapping";
    
    // 同步策略配置
    private int syncDays = 7; // 時間範圍（天）
    private int maxRecordsPerSync = 10000; // 每次同步最大記錄數
    private String timeField = "charge_start_time"; // 時間字段
    
    // 即時同步配置
    private RealTime realTime = new RealTime();
    
    @Data
    public static class RealTime {
        private boolean enabled = true;
        private int intervalSeconds = 300; // 5分鐘間隔
        private int maxBatchSize = 1000; // 即時同步的批次大小
    }
}
