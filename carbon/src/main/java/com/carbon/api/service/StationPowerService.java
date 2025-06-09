package com.carbon.api.service;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.StationPowerRequest;
import com.carbon.api.entity.CarbonStationPower;

import java.util.List;

/**
 * 場站負載服務接口
 */
public interface StationPowerService {
    /**
     * 更新場站負載信息
     */
    ResultResponse updateStationPower(StationPowerRequest request);
    
    /**
     * 獲取場站負載信息
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param stationId 場站ID，如果為null則獲取全部場站的負載信息
     * @return 場站負載列表
     */
    List<CarbonStationPower> getStationPower(String vendorId, String stationId);
} 