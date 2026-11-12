package com.carbon.api.service.impl;

import com.carbon.api.dto.SyncResult;
import com.carbon.api.dto.external.cpo.CpoDatasetResponse;
import com.carbon.api.entity.CarbonCpoConfig;
import com.carbon.api.repository.CpoConfigRepository;
import com.carbon.api.service.CpoApiClientService;
import com.carbon.api.service.CpoConfigService;
import com.carbon.api.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CpoConfigServiceImpl implements CpoConfigService {

    @Autowired
    private CpoConfigRepository cpoConfigRepository;

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private CpoApiClientService cpoApiClientService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<CarbonCpoConfig> getAllConfigs() {
        return cpoConfigRepository.findAll();
    }

    @Override
    public CarbonCpoConfig getByVendorId(String vendorId) {
        if (vendorId == null) return null;
        return cpoConfigRepository.findByVendorId(vendorId.trim()).orElse(null);
    }

    @Override
    @Transactional
    public CarbonCpoConfig saveOrUpdate(CarbonCpoConfig input) {
        if (input.getVendorId() == null || input.getVendorId().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor ID 不得為空");
        }
        if (input.getApiUrl() == null || input.getApiUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("API URL 端點不得為空");
        }

        String cleanVendorId = input.getVendorId().trim();
        String now = LocalDateTime.now().format(DTF);

        Optional<CarbonCpoConfig> existingOpt = cpoConfigRepository.findByVendorId(cleanVendorId);
        CarbonCpoConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            config.setVendorName(input.getVendorName() != null ? input.getVendorName().trim() : config.getVendorName());
            config.setApiUrl(input.getApiUrl().trim());
            if (input.getToken() != null) {
                config.setToken(input.getToken().trim());
            }
            if (input.getEnabled() != null) {
                config.setEnabled(input.getEnabled());
            }
            if (input.getSyncIntervalMins() != null && input.getSyncIntervalMins() > 0) {
                config.setSyncIntervalMins(input.getSyncIntervalMins());
            }
            config.setUpdateTime(now);
        } else {
            config = CarbonCpoConfig.builder()
                    .vendorId(cleanVendorId)
                    .vendorName(input.getVendorName() != null ? input.getVendorName().trim() : cleanVendorId)
                    .apiUrl(input.getApiUrl().trim())
                    .token(input.getToken() != null ? input.getToken().trim() : "")
                    .enabled(input.getEnabled() != null ? input.getEnabled() : "1")
                    .syncIntervalMins(input.getSyncIntervalMins() != null ? input.getSyncIntervalMins() : 15)
                    .createTime(now)
                    .updateTime(now)
                    .build();
        }

        CarbonCpoConfig saved = cpoConfigRepository.save(config);
        log.info("[CPO Config] Saved configuration for vendor: {}", cleanVendorId);
        return saved;
    }

    @Override
    @Transactional
    public void deleteByVendorId(String vendorId) {
        if (vendorId == null || vendorId.trim().isEmpty()) return;
        cpoConfigRepository.deleteByVendorId(vendorId.trim());
        log.info("[CPO Config] Deleted configuration for vendor: {}", vendorId);
    }

    @Override
    @Transactional
    public CarbonCpoConfig toggleEnabled(String vendorId) {
        CarbonCpoConfig config = getByVendorId(vendorId);
        if (config == null) {
            throw new IllegalArgumentException("找不到業者設定: " + vendorId);
        }
        String current = config.getEnabled();
        config.setEnabled("1".equals(current) ? "0" : "1");
        config.setUpdateTime(LocalDateTime.now().format(DTF));
        return cpoConfigRepository.save(config);
    }

    @Override
    @Transactional
    public SyncResult triggerSync(String vendorId, String startTime, String endTime) {
        CarbonCpoConfig config = getByVendorId(vendorId);
        if (config == null) {
            return SyncResult.failure("找不到業者設定: " + vendorId, "Vendor config not found");
        }

        String now = LocalDateTime.now().format(DTF);
        String targetEnd = (endTime != null && !endTime.trim().isEmpty()) ? endTime : now;
        String targetStart = (startTime != null && !startTime.trim().isEmpty()) ? startTime
                : LocalDateTime.now().minusHours(24).format(DTF);

        SyncResult result = dataSyncService.syncFromCpoApi(
                config.getApiUrl(),
                config.getToken(),
                config.getVendorId(),
                targetStart,
                targetEnd
        );

        // 更新同步結果至設定檔記錄
        config.setLastSyncTime(now);
        config.setLastSyncStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        config.setLastSyncMessage(result.getMessage());
        config.setUpdateTime(now);
        cpoConfigRepository.save(config);

        return result;
    }

    @Override
    public Map<String, Object> testConnection(String apiUrl, String token, String vendorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String now = LocalDateTime.now().format(DTF);
            String oneHourAgo = LocalDateTime.now().minusHours(1).format(DTF);

            CpoDatasetResponse response = cpoApiClientService.fetchDataset(
                    apiUrl, token, vendorId, oneHourAgo, now, 1, 10
            );

            if (response != null && "0000".equals(response.getResultCode())) {
                result.put("success", true);
                result.put("message", "連線成功！對方 API 回應正常。");
                result.put("resultCode", response.getResultCode());
                result.put("resultMsg", response.getResultMsg());
                if (response.getData() != null) {
                    result.put("stationsFound", response.getData().getStations() != null ? response.getData().getStations().size() : 0);
                    result.put("chargersFound", response.getData().getChargers() != null ? response.getData().getChargers().size() : 0);
                    result.put("transactionsFound", response.getData().getTransactions() != null ? response.getData().getTransactions().size() : 0);
                }
            } else {
                result.put("success", false);
                result.put("message", "API 連線失敗或回傳非 0000 代碼: " + (response != null ? response.getResultMsg() : "無回應內容"));
                result.put("resultCode", response != null ? response.getResultCode() : "9999");
            }
        } catch (Exception e) {
            log.error("[CPO Config] Test connection failed: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "連線發生異常: " + e.getMessage());
        }
        return result;
    }
}
