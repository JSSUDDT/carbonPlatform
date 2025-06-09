package com.carbon.api.service;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.StationRequest;
import com.carbon.api.entity.CarbonStation;

import java.util.List;

/**
 * 充電站服務接口
 */
public interface StationService {
    /**
     * 更新充電站信息
     */
    ResultResponse updateStationInfo(StationRequest request);
    
    /**
     * 獲取充電站信息
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @return 充電站列表
     */
    List<CarbonStation> getStationInfo(String vendorId);

    /**
     * 根據充電站ID獲取單一充電站信息
     * 
     * @param stationId 充電站ID
     * @return 充電站信息，如果不存在則返回null
     */
    CarbonStation getStationInfoById(String stationId);
    
    /**
     * 獲取充電站信息，並支持搜索和狀態篩選
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param search 搜索關鍵字，用於在站點ID、名稱、城市和地址中搜索
     * @param status 狀態篩選，可選值為"1"(啟用)或"0"(停用)
     * @return 符合條件的充電站列表
     */
    List<CarbonStation> getStationInfo(String vendorId, String search, String status);
} 