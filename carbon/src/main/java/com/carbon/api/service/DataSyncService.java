package com.carbon.api.service;

import com.carbon.api.dto.SyncResult;

public interface DataSyncService {
    
    /**
     * 執行數據同步
     * @return 同步結果
     */
    SyncResult syncData();
    
    /**
     * 執行手動同步
     * @return 同步結果
     */
    SyncResult manualSync();
    
    /**
     * 獲取同步狀態
     * @return 同步狀態信息
     */
    String getSyncStatus();
}
