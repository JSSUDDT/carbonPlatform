package com.carbon.api.controller;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.service.StationCountryUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理員專用 API 控制器
 * 提供系統管理相關功能
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private StationCountryUpdateService stationCountryUpdateService;
    
    /**
     * 批次更新所有充電站的國家資訊
     * 
     * @param forceUpdate 是否強制更新所有充電站（包括已有國家代碼的），預設為 false
     *                    false: 只更新沒有國家代碼或為 "NA" 的充電站
     *                    true: 更新所有充電站（重新調用 API）
     * @param limit 限制處理的站點數量，預設為 0（處理所有站點）
     *              大於 0 的數字：只處理前 N 個需要更新的站點
     * @return 更新結果
     */
    @PostMapping("/stations/update-country")
    public ResponseEntity<ResultResponse> updateAllStationsCountry(
            @RequestParam(value = "forceUpdate", defaultValue = "false") boolean forceUpdate,
            @RequestParam(value = "limit", defaultValue = "0") int limit) {
        
        logger.info("Received batch update request, force update mode: {}, limit: {}", forceUpdate, limit);
        
        // 驗證 limit 參數
        if (limit < 0) {
            logger.warn("Invalid limit parameter: {}, setting to 0 (unlimited)", limit);
            limit = 0;
        }
        
        try {
            StationCountryUpdateService.UpdateResult result = 
                    stationCountryUpdateService.updateAllStationsCountry(forceUpdate, limit);
            
            // 構建回應訊息
            String message = result.getMessage();
            ResultResponse response = new ResultResponse("0000", message);
            
            // 可以選擇將詳細結果放在 response 的 data 中
            // 這裡簡化處理，只返回訊息
            
            logger.info("Batch update completed: {}", message);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error occurred during batch update", e);
            return ResponseEntity.ok(ResultResponse.fail());
        }
    }
    
    /**
     * 獲取批次更新狀態（預留接口，未來可擴展）
     */
    @GetMapping("/stations/update-country/status")
    public ResponseEntity<String> getUpdateStatus() {
        return ResponseEntity.ok("批次更新功能可用");
    }
}

