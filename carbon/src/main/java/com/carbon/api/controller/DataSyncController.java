package com.carbon.api.controller;

import com.carbon.api.config.SyncConfig;
import com.carbon.api.dto.SyncResult;
import com.carbon.api.dto.external.cpo.CpoPullRequest;
import com.carbon.api.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/sync")
public class DataSyncController {
    
    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private SyncConfig syncConfig;
    
    /**
     * 手動觸發數據同步 (舊版 DB 或預設)
     */
    @PostMapping("/manual")
    public ResponseEntity<SyncResult> manualSync() {
        log.info("收到手動同步請求");
        SyncResult result = dataSyncService.manualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 手動觸發 CPO 單一 API 拉取同步
     * 支援 RequestBody 或 RequestParam 指定端點、Token、業者代碼與時間區間
     */
    @PostMapping("/pull")
    public ResponseEntity<SyncResult> pullCpoApi(
            @RequestBody(required = false) CpoPullRequest request,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String apiUrl,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        String targetVendorId = (request != null && request.getVendorId() != null) ? request.getVendorId() : vendorId;
        String targetApiUrl = (request != null && request.getApiUrl() != null) ? request.getApiUrl() : apiUrl;
        String targetToken = (request != null && request.getToken() != null) ? request.getToken() : token;
        String targetStart = (request != null && request.getStartTime() != null) ? request.getStartTime() : startTime;
        String targetEnd = (request != null && request.getEndTime() != null) ? request.getEndTime() : endTime;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (targetEnd == null || targetEnd.trim().isEmpty()) {
            targetEnd = LocalDateTime.now().format(dtf);
        }
        if (targetStart == null || targetStart.trim().isEmpty()) {
            targetStart = LocalDateTime.now().minusDays(syncConfig.getSyncDays()).format(dtf);
        }

        // 若直接指定了 apiUrl
        if (targetApiUrl != null && !targetApiUrl.trim().isEmpty()) {
            if (targetVendorId == null || targetVendorId.trim().isEmpty()) {
                targetVendorId = "MOCK_VENDOR";
            }
            log.info("觸發指定 CPO API 同步: vendorId={}, URL={}, range: {} ~ {}", targetVendorId, targetApiUrl, targetStart, targetEnd);
            SyncResult result = dataSyncService.syncFromCpoApi(targetApiUrl, targetToken, targetVendorId, targetStart, targetEnd);
            return ResponseEntity.ok(result);
        }

        // 若指定了 vendorId 但未給 URL，自設定檔搜尋
        if (targetVendorId != null && !targetVendorId.trim().isEmpty()) {
            for (SyncConfig.CpoApiConfig cpo : syncConfig.getCpoSources()) {
                if (targetVendorId.equalsIgnoreCase(cpo.getVendorId())) {
                    log.info("觸發配置清單之 CPO 同步: vendorId={}, URL={}", cpo.getVendorId(), cpo.getApiUrl());
                    SyncResult result = dataSyncService.syncFromCpoApi(cpo.getApiUrl(), cpo.getToken(), cpo.getVendorId(), targetStart, targetEnd);
                    return ResponseEntity.ok(result);
                }
            }
            return ResponseEntity.badRequest().body(SyncResult.failure("找不到該廠商之 CPO API 配置: " + targetVendorId, "No matching config found"));
        }

        // 若皆未指定，同步所有啟用之 CPO
        log.info("觸發全量 CPO API 同步...");
        SyncResult result = dataSyncService.syncAllConfiguredCpos();
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
