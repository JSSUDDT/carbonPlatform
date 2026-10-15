package com.carbon.api.service.impl;

import com.carbon.api.config.SyncConfig;
import com.carbon.api.dto.SyncResult;
import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {
    
    @Autowired
    private SyncConfig syncConfig;
    
    @Autowired
    @Qualifier("externalJdbcTemplate")
    private JdbcTemplate externalJdbcTemplate;
    
    @Autowired
    private ChargeRepository chargeRepository;
    
    // 外部數據查詢SQL
    private static final String EXTERNAL_QUERY = 
        "SELECT DISTINCT tm.station_id as stationId, " +
        "       tm.device_key as chargerId, " +
        "       tm.connect_id as connectId, " +
        "       tm.trans_no as transNo, " +
        "       tm.invoice_number as invoiceNo, " +
        "       tm.invoice_date as invoiceDate, " +
        "       tm.line_token_id as customerId, " +
        "       '1' as carbonCredit, " +
        "       tm.charge_start_time as startTime, " +
        "       tm.charge_end_time as endTime, " +
        "       tm.kwh as kwh, " +
        "       '' as carType, " +
        "       '' as carTonnage, " +
        "       tm.car_plate as carNo " +
        "FROM %s.trans_mapping tm " +
        "INNER JOIN %s.carbon_station cs ON tm.station_id = cs.station_id " +
        "INNER JOIN %s.carbon_charger cc ON tm.device_key = cc.charger_id " +
        "    AND tm.connect_id = cc.connect_id " +
        "WHERE tm.charge_start_time >= DATE_SUB(NOW(), INTERVAL %d DAY) " +
        "  AND tm.charge_start_time IS NOT NULL " +
        "  AND tm.charge_end_time IS NOT NULL " +
        "  AND tm.charge_start_time < tm.charge_end_time " +
        "  AND tm.charge_start_time != 'Invalid Date' " +
        "  AND tm.charge_end_time != 'Invalid Date' " +
        "  AND tm.charge_start_time != '' " +
        "  AND tm.charge_end_time != '' " +
        "ORDER BY tm.charge_start_time DESC " +
        "LIMIT %d";
    
    @Override
    @Transactional
    public SyncResult syncData() {
        try {
            log.info("Starting data synchronization...");
            
            // 獲取外部數據
            List<Map<String, Object>> externalData = fetchExternalTransactionData();
            if (externalData.isEmpty()) {
                log.info("No data to synchronize");
                return SyncResult.success(0, 0, 0);
            }
            
            log.info("Retrieved {} external data records", externalData.size());
            
            // 執行同步
            int syncedCount = 0;
            int skippedCount = 0;
            List<String> successfulTransNos = new ArrayList<>();
            
            for (Map<String, Object> data : externalData) {
                try {
                    if (syncTransactionRecord(data)) {
                        syncedCount++;
                        // 收集成功的交易號
                        String transNo = (String) data.get("transNo");
                        if (transNo != null) {
                            successfulTransNos.add(transNo);
                        }
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync record: {}", data, e);
                    skippedCount++;
                }
            }
            
            // 打印同步結果，包含成功的交易號
            if (syncedCount > 0) {
                log.info("Synchronization completed: {} successful, {} skipped. Successful transactions: {}", 
                    syncedCount, skippedCount, String.join(", ", successfulTransNos));
            } else {
                log.info("Synchronization completed: {} successful, {} skipped", syncedCount, skippedCount);
            }
            
            return SyncResult.success(externalData.size(), syncedCount, skippedCount);
            
        } catch (Exception e) {
            log.error("Data synchronization failed", e);
            return SyncResult.failure("Synchronization failed: " + e.getMessage(), e.toString());
        }
    }
    
    @Override
    public SyncResult manualSync() {
        log.info("Executing manual synchronization...");
        return syncData();
    }
    
    @Override
    public String getSyncStatus() {
        return "Data synchronization service is running, configuration: " + syncConfig.toString();
    }
    
    /**
     * 獲取外部交易數據
     */
    private List<Map<String, Object>> fetchExternalTransactionData() {
        String sql = String.format(EXTERNAL_QUERY,
                syncConfig.getSourceSchema(),
                "ccms_sit", // 系統數據庫schema
                "ccms_sit", // 系統數據庫schema
                syncConfig.getSyncDays(),
                syncConfig.getMaxRecordsPerSync()
        );
        
        log.debug("Executing query SQL: {}", sql);
        return externalJdbcTemplate.queryForList(sql);
    }
    
    /**
     * 同步單條交易記錄
     */
    private boolean syncTransactionRecord(Map<String, Object> data) {
        try {
            String transNo = (String) data.get("transNo");
            String startTime = (String) data.get("startTime");
            String endTime = (String) data.get("endTime");
            
            // 檢查日期有效性
            if (isInvalidDate(startTime) || isInvalidDate(endTime)) {
                log.debug("Skipping invalid date transaction record: {}, startTime: {}, endTime: {}", transNo, startTime, endTime);
                return false;
            }
            
            // 檢查是否已存在
            if (chargeRepository.findByTransNo(transNo) != null) {
                log.debug("Transaction record already exists, skipping: {}", transNo);
                return false;
            }
            
            // 創建新的交易記錄
            CarbonCharge charge = new CarbonCharge();
            charge.setTransNo(transNo);
            charge.setVendorId(getVendorIdByStation((String) data.get("stationId")));
            charge.setStationId((String) data.get("stationId"));
            charge.setChargerId((String) data.get("chargerId"));
            charge.setConnectId((String) data.get("connectId"));
            charge.setInvoiceNo((String) data.get("invoiceNo"));
            charge.setInvoiceDate(formatDateTime(parseDateTime((String) data.get("invoiceDate"))));
            charge.setCustomerId((String) data.get("customerId"));
            charge.setCarbonCredit((String) data.get("carbonCredit"));
            charge.setStartTime(formatDateTime(parseDateTime(startTime)));
            charge.setEndTime(formatDateTime(parseDateTime(endTime)));
            charge.setKwh(formatDouble(parseDouble(data.get("kwh"))));
            charge.setCarType((String) data.get("carType"));
            charge.setCarTonnage((String) data.get("carTonnage"));
            charge.setCarNo((String) data.get("carNo"));
            
            // 設置同步時間
            String currentTime = formatDateTime(LocalDateTime.now());
            charge.setCreateTime(currentTime);
            charge.setUpdateTime(currentTime);
            
            // 保存到數據庫
            chargeRepository.save(charge);
            log.debug("Successfully synced transaction record: {}", transNo);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to sync transaction record: {}", data, e);
            return false;
        }
    }
    
    /**
     * 根據充電站ID獲取廠商ID
     */
    private String getVendorIdByStation(String stationId) {
        try {
            String sql = "SELECT vendor_id FROM carbon_station WHERE station_id = ?";
            String vendorId = externalJdbcTemplate.queryForObject(sql, String.class, stationId);
            if (vendorId != null) {
                log.debug("Found vendor ID for station {}: {}", stationId, vendorId);
                return vendorId;
            } else {
                log.warn("Vendor ID not found for station {}: {}", stationId);
                return "unknown_vendor";
            }
        } catch (Exception e) {
            log.error("Failed to query vendor ID for station {}: {}", stationId, e);
            return "error_vendor";
        }
    }
    
    /**
     * 解析日期時間字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 嘗試多種日期格式
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (Exception e) {
                    // 繼續嘗試下一個格式
                }
            }
            
            log.warn("Could not parse date/time: {}", dateTimeStr);
            return null;
            
        } catch (Exception e) {
            log.error("Failed to parse date/time: {}", dateTimeStr, e);
            return null;
        }
    }
    
    /**
     * 解析數值
     */
    private Double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse value: {}", value);
            return 0.0;
        }
    }

    /**
     * 格式化日期時間為字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 格式化數值為字符串
     */
    private String formatDouble(Double value) {
        if (value == null) {
            return "0.0";
        }
        return value.toString();
    }
    
    /**
     * 檢查日期是否為無效值
     */
    private boolean isInvalidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return true;
        }
        
        String trimmedDate = dateStr.trim();
        
        // 檢查是否為 "Invalid Date"
        if ("Invalid Date".equalsIgnoreCase(trimmedDate)) {
            return true;
        }
        
        // 檢查是否為空字符串
        if ("".equals(trimmedDate)) {
            return true;
        }
        
        // 檢查是否為 "null" 字符串
        if ("null".equalsIgnoreCase(trimmedDate)) {
            return true;
        }
        
        // 檢查是否為 "undefined" 字符串
        if ("undefined".equalsIgnoreCase(trimmedDate)) {
            return true;
        }
        
        return false;
    }
}
