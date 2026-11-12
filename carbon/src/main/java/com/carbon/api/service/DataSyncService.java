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
    
    /**
     * 從指定的 CPO API 端點執行單一數據集同步 (站點+設備+交易+Verra 5455防呆+帳號自動開通)
     */
    SyncResult syncFromCpoApi(String apiUrl, String token, String vendorId, String startTime, String endTime);
    
    /**
     * 同步所有在設定檔中啟用的 CPO 業者
     */
    SyncResult syncAllConfiguredCpos();
    
    /**
     * 確保廠商帳號存在（若為新業者則自動以預設密碼建立，實現零人工作業）
     */
    void ensureVendorExists(String vendorId);
}
