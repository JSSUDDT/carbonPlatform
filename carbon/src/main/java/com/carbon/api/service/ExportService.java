package com.carbon.api.service;

import java.io.ByteArrayOutputStream;

/**
 * 匯出服務接口
 */
public interface ExportService {
    
    /**
     * 匯出充電記錄為 Excel
     * 
     * @param vendorId 廠商ID
     * @param stationId 充電站ID（可選）
     * @param startTimeFrom 開始時間（可選）
     * @param startTimeTo 結束時間（可選）
     * @param language 語言（zh_TW 或 en）
     * @return Excel 檔案的位元組陣列
     */
    ByteArrayOutputStream exportChargeInfoToExcel(
            String vendorId, 
            String stationId, 
            String startTimeFrom, 
            String startTimeTo,
            String language) throws Exception;
}

