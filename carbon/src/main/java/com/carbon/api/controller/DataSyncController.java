package com.carbon.api.controller;

import com.carbon.api.dto.SyncResult;
import com.carbon.api.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sync")
public class DataSyncController {
    
    @Autowired
    private DataSyncService dataSyncService;
    
    /**
     * 手動觸發數據同步
     */
    @PostMapping("/manual")
    public ResponseEntity<SyncResult> manualSync() {
        log.info("收到手動同步請求");
        SyncResult result = dataSyncService.manualSync();
        return ResponseEntity.ok(result);
    }
    
    /**
     * 獲取同步狀態
     */
    @GetMapping("/status")
    public ResponseEntity<String> getSyncStatus() {
        String status = dataSyncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("數據同步服務運行正常");
    }
}
