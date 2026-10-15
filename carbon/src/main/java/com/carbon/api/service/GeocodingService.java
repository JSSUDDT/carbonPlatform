package com.carbon.api.service;

/**
 * 地理編碼服務接口
 * 用於根據經緯度獲取國家資訊
 */
public interface GeocodingService {
    /**
     * 根據經緯度獲取國家代碼（ISO 3166-1 alpha-2）
     * 
     * @param latitude 緯度
     * @param longitude 經度
     * @return 國家代碼（如 "TW", "US", "CN"），如果無法獲取則返回 "NA"
     */
    String getCountryCode(String latitude, String longitude);
}

