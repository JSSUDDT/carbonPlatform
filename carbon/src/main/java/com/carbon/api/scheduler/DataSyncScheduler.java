package com.carbon.api.scheduler;

import com.carbon.api.config.SyncConfig;
import com.carbon.api.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataSyncScheduler {
    
    @Autowired
    private DataSyncService dataSyncService;
    
    @Autowired
    private SyncConfig syncConfig;
    
    /**
     * 定時執行數據同步
     * 每4分鐘執行一次
     */
    @Scheduled(cron = "${sync.cron:0 */4 * * * ?}")
    public void scheduledDataSync() {
        // 檢查同步功能是否啟用
        if (!syncConfig.isEnabled()) {
            log.debug("Data sync is disabled, skipping this execution");
            return;
        }
        
        try {
            log.info("Scheduled task triggered data synchronization...");
            dataSyncService.syncData();
        } catch (Exception e) {
            log.error("Scheduled data synchronization failed", e);
        }
    }
}
