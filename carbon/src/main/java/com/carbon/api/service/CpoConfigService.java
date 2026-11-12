package com.carbon.api.service;

import com.carbon.api.dto.SyncResult;
import com.carbon.api.entity.CarbonCpoConfig;

import java.util.List;
import java.util.Map;

public interface CpoConfigService {

    List<CarbonCpoConfig> getAllConfigs();

    CarbonCpoConfig getByVendorId(String vendorId);

    CarbonCpoConfig saveOrUpdate(CarbonCpoConfig config);

    void deleteByVendorId(String vendorId);

    CarbonCpoConfig toggleEnabled(String vendorId);

    SyncResult triggerSync(String vendorId, String startTime, String endTime);

    Map<String, Object> testConnection(String apiUrl, String token, String vendorId);
}
