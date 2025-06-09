package com.carbon.api.service;

import com.carbon.api.dto.ChargerRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.entity.CarbonCharger;

import java.util.List;

/**
 * 充電樁服務接口
 */
public interface ChargerService {
    /**
     * 更新充電樁信息
     */
    ResultResponse updateChargerInfo(ChargerRequest request);
    
    /**
     * 獲取充電樁信息
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param stationId 場站ID，如果為null則獲取全部場站的充電樁
     * @return 充電樁列表
     */
    List<CarbonCharger> getChargerInfo(String vendorId, String stationId);

    /**
     * 根據充電樁ID獲取充電樁信息
     * 
     * @param chargerId 充電樁ID
     * @return 充電樁信息列表，如果不存在則返回空列表
     */
    List<CarbonCharger> getChargerInfoById(String chargerId);
    
    /**
     * 根據充電樁ID和連接頭ID獲取單一充電樁信息
     * 
     * @param chargerId 充電樁ID
     * @param connectId 連接頭ID
     * @return 充電樁信息，如果不存在則返回null
     */
    CarbonCharger getChargerInfoByIdAndConnectId(String chargerId, String connectId);

    /**
     * 獲取充電樁信息，並支持搜索和狀態篩選
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param stationId 場站ID，如果為null則獲取全部場站的充電樁
     * @param search 搜索關鍵字，用於在充電樁ID、類型、品牌中搜索
     * @param status 狀態篩選，可選值為"1"(啟用)或"0"(未啟用)
     * @return 符合條件的充電樁列表
     */
    List<CarbonCharger> getChargerInfo(String vendorId, String stationId, String search, String status);
} 