package com.carbon.api.service;

import com.carbon.api.dto.ChargeRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.entity.CarbonCharge;

import java.util.List;

/**
 * 充電交易服務接口
 */
public interface ChargeService {
    /**
     * 更新充電交易信息
     */
    ResultResponse updateChargeInfo(ChargeRequest request);
    
    /**
     * 獲取充電交易信息
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param stationId 場站ID，如果為null則獲取全部場站的交易記錄
     * @param chargerId 充電樁ID，如果為null則獲取全部充電樁的交易記錄
     * @return 充電交易列表
     */
    List<CarbonCharge> getChargeInfo(String vendorId, String stationId, String chargerId);

    /**
     * 根據交易編號獲取單筆充電交易信息
     * 
     * @param transNo 交易編號
     * @return 充電交易記錄
     */
    CarbonCharge getChargeInfoByTransNo(String transNo);
    
    /**
     * 獲取充電交易信息並按開始時間過濾
     * 
     * @param vendorId 廠商ID，如果為null則根據用戶權限自動獲取
     * @param stationId 場站ID，如果為null則獲取全部場站的交易記錄
     * @param chargerId 充電樁ID，如果為null則獲取全部充電樁的交易記錄
     * @param startTimeFrom 開始時間下限，如果為null則不設下限
     * @param startTimeTo 開始時間上限，如果為null則不設上限
     * @return 符合條件的充電交易列表
     */
    List<CarbonCharge> getChargeInfoWithTimeRange(String vendorId, String stationId, String chargerId,
                                              String startTimeFrom, String startTimeTo);
} 