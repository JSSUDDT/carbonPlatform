package com.carbon.api.controller;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.SyncResult;
import com.carbon.api.entity.CarbonCpoConfig;
import com.carbon.api.service.CpoConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cpo-config")
public class CpoConfigController {

    @Autowired
    private CpoConfigService cpoConfigService;

    /**
     * 獲取所有業者 API 連線設定清單
     */
    @GetMapping
    public ResponseEntity<List<CarbonCpoConfig>> getAllConfigs() {
        return ResponseEntity.ok(cpoConfigService.getAllConfigs());
    }

    /**
     * 根據 vendorId 獲取特定業者 API 連線設定
     */
    @GetMapping("/{vendorId}")
    public ResponseEntity<?> getConfig(@PathVariable String vendorId) {
        CarbonCpoConfig config = cpoConfigService.getByVendorId(vendorId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * 新增或更新業者 API 連線設定
     */
    @PostMapping
    public ResponseEntity<?> saveOrUpdate(@RequestBody CarbonCpoConfig config) {
        try {
            CarbonCpoConfig saved = cpoConfigService.saveOrUpdate(config);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResultResponse("0001", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to save CPO config: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ResultResponse("9999", "儲存失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除業者 API 連線設定
     */
    @DeleteMapping("/{vendorId}")
    public ResponseEntity<ResultResponse> deleteConfig(@PathVariable String vendorId) {
        try {
            cpoConfigService.deleteByVendorId(vendorId);
            return ResponseEntity.ok(ResultResponse.success());
        } catch (Exception e) {
            log.error("Failed to delete CPO config for vendor {}: {}", vendorId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResultResponse.fail());
        }
    }

    /**
     * 切換業者 API 連線啟用狀態 (1 / 0)
     */
    @PostMapping("/{vendorId}/toggle")
    public ResponseEntity<?> toggleEnabled(@PathVariable String vendorId) {
        try {
            CarbonCpoConfig updated = cpoConfigService.toggleEnabled(vendorId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResultResponse("0001", e.getMessage()));
        }
    }

    /**
     * 觸發立即手動同步該業者數據
     */
    @PostMapping("/{vendorId}/sync")
    public ResponseEntity<SyncResult> triggerSync(
            @PathVariable String vendorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("管理員手動觸發業者 {} API 數據同步 (區間: {} ~ {})", vendorId, startTime, endTime);
        SyncResult result = cpoConfigService.triggerSync(vendorId, startTime, endTime);
        return ResponseEntity.ok(result);
    }

    /**
     * 測試與業者 API 端點的連線狀況 (不儲存資料)
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String apiUrl = request.get("apiUrl");
        String token = request.get("token");
        String vendorId = request.get("vendorId");
        log.info("測試業者 API 連線: vendorId={}, apiUrl={}", vendorId, apiUrl);
        Map<String, Object> result = cpoConfigService.testConnection(apiUrl, token, vendorId);
        return ResponseEntity.ok(result);
    }
}
