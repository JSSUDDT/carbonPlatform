package com.carbon.api.service;

import com.carbon.api.dto.external.cpo.CpoDatasetData;
import com.carbon.api.dto.external.cpo.CpoDatasetResponse;

public interface CpoApiClientService {
    
    /**
     * 呼叫業者端單一查詢 API 單頁數據
     */
    CpoDatasetResponse fetchDataset(String apiUrl, String token, String vendorId, String startTime, String endTime, Integer page, Integer pageSize);
    
    /**
     * 自動分頁迴圈拉取該時段之完整數據（自動匯整站點、設備與全部交易）
     */
    CpoDatasetData fetchAllPages(String apiUrl, String token, String vendorId, String startTime, String endTime, Integer pageSize);
}
