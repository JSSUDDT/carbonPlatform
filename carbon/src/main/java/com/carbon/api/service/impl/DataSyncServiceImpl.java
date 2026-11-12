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
import java.util.Set;
import java.util.HashSet;
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

    @Autowired
    private com.carbon.api.repository.ChargerRepository chargerRepository;

    @Autowired
    private com.carbon.api.repository.StationRepository stationRepository;

    @Autowired
    private com.carbon.api.repository.VendorRepository vendorRepository;

    @Autowired
    private com.carbon.api.repository.CpoConfigRepository cpoConfigRepository;

    @Autowired
    private com.carbon.api.service.GeocodingService geocodingService;

    @Autowired
    private com.carbon.api.service.CpoApiClientService cpoApiClientService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
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
            updateChargerFirstTransactionTime(charge.getVendorId(), charge.getStationId(), charge.getChargerId(), charge.getConnectId(), charge.getStartTime());
            log.debug("Successfully synced transaction record: {}", transNo);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to sync transaction record: {}", data, e);
            return false;
        }
    }

    /**
     * 同步過程中自動判定並更新充電樁槍頭的首次交易時間
     */
    private void updateChargerFirstTransactionTime(String vendorId, String stationId, String chargerId, String connectId, String transStartTime) {
        try {
            if (vendorId == null || stationId == null || chargerId == null || connectId == null || transStartTime == null) {
                return;
            }
            com.carbon.api.entity.CarbonCharger charger = chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(vendorId, stationId, chargerId, connectId);
            if (charger == null) {
                return;
            }

            // 檢查是否大於等於首次啟用日期 (以台北時間 00:00:00 對應之 UTC 時間比較)
            String enableDate = charger.getFirstEnableDate();
            if (enableDate != null && !enableDate.trim().isEmpty()) {
                String minTime = convertEnableDateToUtc(enableDate);
                if (minTime != null && transStartTime.compareTo(minTime) < 0) {
                    return;
                }
            }

            String currentFirstTx = charger.getFirstTransactionTime();
            if (currentFirstTx == null || currentFirstTx.trim().isEmpty()) {
                charger.setFirstTransactionTime(transStartTime);
                charger.setUpdateTime(formatDateTime(LocalDateTime.now()));
                chargerRepository.save(charger);
                log.info("Auto set firstTransactionTime during sync for charger {} connect {}: {}", chargerId, connectId, transStartTime);
            } else if (transStartTime.compareTo(currentFirstTx) < 0) {
                charger.setFirstTransactionTime(transStartTime);
                charger.setUpdateTime(formatDateTime(LocalDateTime.now()));
                chargerRepository.save(charger);
                log.info("Calibrated earlier firstTransactionTime during sync for charger {} connect {}: {} -> {}", chargerId, connectId, currentFirstTx, transStartTime);
            }
        } catch (Exception e) {
            log.warn("Failed to auto-update charger firstTransactionTime: {}", e.getMessage());
        }
    }

    /**
     * 將首次啟用日期 (yyyy-MM-dd) 視為台北時間 00:00:00，轉為 UTC+0 比較基準
     * 例如：2026-01-01 -> 2025-12-31 16:00:00 UTC
     */
    private String convertEnableDateToUtc(String enableDate) {
        if (enableDate == null || enableDate.trim().isEmpty()) {
            return null;
        }
        String trimmed = enableDate.trim();
        try {
            if (trimmed.length() == 10) {
                trimmed += " 00:00:00";
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDt = LocalDateTime.parse(trimmed, dtf);
            java.time.ZonedDateTime taipeiZoned = localDt.atZone(java.time.ZoneId.of("Asia/Taipei"));
            java.time.ZonedDateTime utcZoned = taipeiZoned.withZoneSameInstant(java.time.ZoneId.of("UTC"));
            return utcZoned.format(dtf);
        } catch (Exception e) {
            return enableDate;
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

    @Override
    @Transactional
    public void ensureVendorExists(String vendorId) {
        if (vendorId == null || vendorId.trim().isEmpty()) {
            return;
        }
        String cleanVendorId = vendorId.trim();
        com.carbon.api.entity.CarbonVendor vendor = vendorRepository.findByVendorId(cleanVendorId);
        if (vendor == null) {
            String initialPassword = "Pass@" + cleanVendorId + "2026!";
            com.carbon.api.entity.CarbonVendor newVendor = new com.carbon.api.entity.CarbonVendor();
            newVendor.setVendorId(cleanVendorId);
            newVendor.setPassword(passwordEncoder.encode(initialPassword));
            newVendor.setStatus("1"); // 預設啟用
            newVendor.setRole("ROLE_USER");
            String now = formatDateTime(LocalDateTime.now());
            newVendor.setCreateTime(now);
            newVendor.setUpdateTime(now);
            vendorRepository.save(newVendor);

            log.warn("=================================================================");
            log.warn(">>> [Vendor Auto-Provisioned] 新業者帳號已由 CPO API 同步自動建立！");
            log.warn(">>> 業者代號 (vendorId): {}", cleanVendorId);
            log.warn(">>> 初始預設密碼       : {}", initialPassword);
            log.warn("=================================================================");
        }
    }

    @Override
    @Transactional
    public SyncResult syncFromCpoApi(String apiUrl, String token, String vendorId, String startTime, String endTime) {
        try {
            log.info("Starting CPO API data sync for vendor: {}, URL: {}, range: {} ~ {}", vendorId, apiUrl, startTime, endTime);

            if (vendorId == null || vendorId.trim().isEmpty()) {
                return SyncResult.failure("vendorId cannot be empty", "Missing vendorId parameter");
            }
            String cleanVendorId = vendorId.trim();

            // Step 0: 自動審查並開通廠商帳號
            ensureVendorExists(cleanVendorId);

            // 呼叫 CPO Client 自動分頁拉取數據
            com.carbon.api.dto.external.cpo.CpoDatasetData data = cpoApiClientService.fetchAllPages(
                    apiUrl, token, cleanVendorId, startTime, endTime, 1000
            );

            if (data == null) {
                return SyncResult.failure("Failed to retrieve dataset from CPO API", "CPO response data is null");
            }

            // Step 1: 站點同步 (Stations)
            int stationCount = 0;
            if (data.getStations() != null) {
                for (com.carbon.api.dto.external.cpo.CpoStationDto st : data.getStations()) {
                    if (st == null || st.getStationId() == null || st.getStationId().trim().isEmpty()) {
                        continue;
                    }
                    String stId = st.getStationId().trim();
                    com.carbon.api.entity.CarbonStation station = stationRepository.findByVendorIdAndStationId(cleanVendorId, stId);
                    if (station == null) {
                        station = new com.carbon.api.entity.CarbonStation();
                        station.setVendorId(cleanVendorId);
                        station.setStationId(stId);
                        station.setStationName(st.getStationName() != null ? st.getStationName() : stId);
                        station.setGpsLatitude(st.getGpsLatitude());
                        station.setGpsLongitude(st.getGpsLongitude());
                        station.setAddress(st.getAddress());
                        station.setStationStatus("1");
                        
                        // 逆地理編碼反查國家代碼
                        if (st.getGpsLatitude() != null && st.getGpsLongitude() != null) {
                            try {
                                String country = geocodingService.getCountryCode(st.getGpsLatitude(), st.getGpsLongitude());
                                station.setCountry((country != null && !country.isEmpty()) ? country : "TW");
                            } catch (Exception e) {
                                station.setCountry("TW");
                            }
                        } else {
                            station.setCountry("TW");
                        }
                        
                        String now = formatDateTime(LocalDateTime.now());
                        station.setCreateTime(now);
                        station.setUpdateTime(now);
                        stationRepository.save(station);
                        stationCount++;
                        log.debug("Auto-created station: {} for vendor {}", stId, cleanVendorId);
                    } else {
                        boolean modified = false;
                        if (st.getStationName() != null && !st.getStationName().equals(station.getStationName())) {
                            station.setStationName(st.getStationName());
                            modified = true;
                        }
                        if (st.getGpsLatitude() != null && !st.getGpsLatitude().equals(station.getGpsLatitude())) {
                            station.setGpsLatitude(st.getGpsLatitude());
                            modified = true;
                        }
                        if (st.getGpsLongitude() != null && !st.getGpsLongitude().equals(station.getGpsLongitude())) {
                            station.setGpsLongitude(st.getGpsLongitude());
                            modified = true;
                        }
                        if (st.getAddress() != null && !st.getAddress().equals(station.getAddress())) {
                            station.setAddress(st.getAddress());
                            modified = true;
                        }
                        if (modified) {
                            station.setUpdateTime(formatDateTime(LocalDateTime.now()));
                            stationRepository.save(station);
                        }
                    }
                }
            }

            // Step 2: 充電槍頭設備同步 (Chargers)
            int chargerCount = 0;
            if (data.getChargers() != null) {
                for (com.carbon.api.dto.external.cpo.CpoChargerDto ch : data.getChargers()) {
                    if (ch == null || ch.getStationId() == null || ch.getChargerId() == null || ch.getConnectId() == null) {
                        continue;
                    }
                    String stId = ch.getStationId().trim();
                    String chId = ch.getChargerId().trim();
                    String connId = ch.getConnectId().trim();

                    com.carbon.api.entity.CarbonCharger charger = chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(
                            cleanVendorId, stId, chId, connId
                    );

                    if (charger == null) {
                        charger = new com.carbon.api.entity.CarbonCharger();
                        charger.setVendorId(cleanVendorId);
                        charger.setStationId(stId);
                        charger.setChargerId(chId);
                        charger.setConnectId(connId);
                        charger.setFirstEnableDate(ch.getFirstEnableDate());
                        charger.setChargerType(ch.getChargerType() != null ? ch.getChargerType() : "DC");
                        charger.setChargerKw(ch.getChargerKw() != null ? ch.getChargerKw() : "120");
                        charger.setChargerStatus("1");
                        String now = formatDateTime(LocalDateTime.now());
                        charger.setCreateTime(now);
                        charger.setUpdateTime(now);
                        chargerRepository.save(charger);
                        chargerCount++;
                        log.debug("Auto-created charger {}-{} for station {}", chId, connId, stId);
                    } else {
                        boolean modified = false;
                        if (ch.getFirstEnableDate() != null && !ch.getFirstEnableDate().trim().isEmpty()
                                && !ch.getFirstEnableDate().equals(charger.getFirstEnableDate())) {
                            charger.setFirstEnableDate(ch.getFirstEnableDate().trim());
                            modified = true;
                        }
                        if (ch.getChargerKw() != null && !ch.getChargerKw().equals(charger.getChargerKw())) {
                            charger.setChargerKw(ch.getChargerKw());
                            modified = true;
                        }
                        if (ch.getChargerType() != null && !ch.getChargerType().equals(charger.getChargerType())) {
                            charger.setChargerType(ch.getChargerType());
                            modified = true;
                        }
                        if (modified) {
                            charger.setUpdateTime(formatDateTime(LocalDateTime.now()));
                            chargerRepository.save(charger);
                        }
                    }
                }
            }

            // Step 3: 交易記錄合規入庫 (Transactions - Verra 5455 防呆)
            List<com.carbon.api.dto.external.cpo.CpoTransactionDto> transactions = data.getTransactions();
            if (transactions == null || transactions.isEmpty()) {
                log.info("No transaction records found in CPO API response for vendor {}", cleanVendorId);
                return SyncResult.builder()
                        .success(true)
                        .message("同步完成（站點:" + stationCount + ", 設備:" + chargerCount + ", 交易:0）")
                        .totalRecords(0)
                        .syncedRecords(0)
                        .skippedRecords(0)
                        .errorRecords(0)
                        .syncTime(LocalDateTime.now())
                        .build();
            }

            int syncedCount = 0;
            int skippedCount = 0;
            List<String> successfulTransNos = new ArrayList<>();

            for (com.carbon.api.dto.external.cpo.CpoTransactionDto tx : transactions) {
                try {
                    String transNo = tx.getTransNo();
                    if (transNo == null || transNo.trim().isEmpty()) {
                        skippedCount++;
                        continue;
                    }
                    transNo = transNo.trim();

                    // 1. 冪等性防重：若已存在則安全略過
                    if (chargeRepository.findByTransNo(transNo) != null) {
                        log.debug("Transaction {} already exists, skipping", transNo);
                        skippedCount++;
                        continue;
                    }

                    // 2. 日期時間有效性檢查
                    String txStartTime = tx.getStartTime();
                    String txEndTime = tx.getEndTime();
                    if (isInvalidDate(txStartTime) || isInvalidDate(txEndTime)) {
                        log.warn("Skipping transaction {} due to invalid date: start={}, end={}", transNo, txStartTime, txEndTime);
                        skippedCount++;
                        continue;
                    }

                    LocalDateTime startDt = parseDateTime(txStartTime);
                    LocalDateTime endDt = parseDateTime(txEndTime);
                    if (startDt == null || endDt == null || !startDt.isBefore(endDt)) {
                        log.warn("Skipping transaction {} due to invalid time sequence: {} -> {}", transNo, txStartTime, txEndTime);
                        skippedCount++;
                        continue;
                    }

                    // 3. Verra 5455: 時長物理極限檢核 (0 < duration <= 72小時)
                    long durationMinutes = java.time.Duration.between(startDt, endDt).toMinutes();
                    if (durationMinutes <= 0 || durationMinutes > 72 * 60) {
                        log.warn("Skipping transaction {} due to duration out of bounds ({} mins)", transNo, durationMinutes);
                        skippedCount++;
                        continue;
                    }

                    // 4. Verra 5455: 電量合理邊界 (0.01 <= kwh <= 1000.00)
                    Double kwhVal = parseDouble(tx.getKwh());
                    if (kwhVal == null || kwhVal < 0.01 || kwhVal > 1000.00) {
                        log.warn("Skipping transaction {} due to abnormal kwh: {}", transNo, kwhVal);
                        skippedCount++;
                        continue;
                    }

                    // 5. Verra 5455: 基準日校驗 (交易開始時間不得早於該槍頭首次啟用日)
                    String stId = tx.getStationId();
                    String chId = tx.getChargerId();
                    String connId = tx.getConnectId();
                    com.carbon.api.entity.CarbonCharger targetCharger = null;
                    if (stId != null && chId != null && connId != null) {
                        targetCharger = chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(
                                cleanVendorId, stId.trim(), chId.trim(), connId.trim()
                        );
                    }

                    if (targetCharger != null && targetCharger.getFirstEnableDate() != null && !targetCharger.getFirstEnableDate().trim().isEmpty()) {
                        String enableDateUtc = convertEnableDateToUtc(targetCharger.getFirstEnableDate());
                        String formattedStart = formatDateTime(startDt);
                        if (enableDateUtc != null && formattedStart.compareTo(enableDateUtc) < 0) {
                            log.warn("Skipping transaction {} because startTime ({}) is earlier than firstEnableDate ({})",
                                    transNo, formattedStart, enableDateUtc);
                            skippedCount++;
                            continue;
                        }
                    }

                    // 構建碳權充電實體入庫
                    CarbonCharge charge = new CarbonCharge();
                    charge.setTransNo(transNo);
                    charge.setVendorId(cleanVendorId);
                    charge.setStationId(stId != null ? stId.trim() : "");
                    charge.setChargerId(chId != null ? chId.trim() : "");
                    charge.setConnectId(connId != null ? connId.trim() : "1");
                    charge.setInvoiceNo(tx.getInvoiceNo());
                    charge.setCarbonCredit("1");
                    charge.setStartTime(formatDateTime(startDt));
                    charge.setEndTime(formatDateTime(endDt));
                    charge.setKwh(formatDouble(kwhVal));
                    charge.setCarType(tx.getCarType() != null ? tx.getCarType() : "");
                    charge.setCarNo(tx.getCarNo() != null ? tx.getCarNo() : "");

                    String currentTime = formatDateTime(LocalDateTime.now());
                    charge.setCreateTime(currentTime);
                    charge.setUpdateTime(currentTime);

                    chargeRepository.save(charge);
                    updateChargerFirstTransactionTime(cleanVendorId, charge.getStationId(), charge.getChargerId(), charge.getConnectId(), charge.getStartTime());

                    syncedCount++;
                    successfulTransNos.add(transNo);
                } catch (Exception e) {
                    log.error("Failed to ingest transaction {}: {}", tx.getTransNo(), e.getMessage());
                    skippedCount++;
                }
            }

            log.info("CPO API Sync completed for vendor {}: stations={}, chargers={}, totalTx={}, syncedTx={}, skippedTx={}",
                    cleanVendorId, stationCount, chargerCount, transactions.size(), syncedCount, skippedCount);

            return SyncResult.builder()
                    .success(true)
                    .message(String.format("CPO API 同步成功 (站點新增/更新:%d, 設備新增/更新:%d, 交易入庫:%d, 略過:%d)",
                            stationCount, chargerCount, syncedCount, skippedCount))
                    .totalRecords(transactions.size())
                    .syncedRecords(syncedCount)
                    .skippedRecords(skippedCount)
                    .errorRecords(0)
                    .syncTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("CPO API Sync failed for vendor {}: {}", vendorId, e.getMessage(), e);
            return SyncResult.failure("CPO API 同步異常: " + e.getMessage(), e.toString());
        }
    }

    @Override
    public SyncResult syncAllConfiguredCpos() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.minusDays(syncConfig.getSyncDays()).format(dtf);
        String endTime = now.format(dtf);

        int totalTx = 0;
        int totalSynced = 0;
        int totalSkipped = 0;
        Set<String> processedVendorIds = new HashSet<>();

        // 1. 優先處理資料庫中設定且啟用 (enabled="1") 的 CPO 來源
        try {
            List<com.carbon.api.entity.CarbonCpoConfig> dbConfigs = cpoConfigRepository.findByEnabled("1");
            if (dbConfigs != null && !dbConfigs.isEmpty()) {
                log.info("Found {} active CPO configurations in database", dbConfigs.size());
                for (com.carbon.api.entity.CarbonCpoConfig cfg : dbConfigs) {
                    try {
                        SyncResult r = syncFromCpoApi(cfg.getApiUrl(), cfg.getToken(), cfg.getVendorId(), startTime, endTime);
                        totalTx += r.getTotalRecords();
                        totalSynced += r.getSyncedRecords();
                        totalSkipped += r.getSkippedRecords();
                        processedVendorIds.add(cfg.getVendorId().toUpperCase());

                        // 更新執行結果回資料庫記錄
                        cfg.setLastSyncTime(now.format(dtf));
                        cfg.setLastSyncStatus(r.isSuccess() ? "SUCCESS" : "FAILED");
                        cfg.setLastSyncMessage(r.getMessage());
                        cfg.setUpdateTime(now.format(dtf));
                        cpoConfigRepository.save(cfg);
                    } catch (Exception e) {
                        log.error("Failed to sync DB-configured CPO: {}", cfg.getVendorId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not query carbon_cpo_config table: {}", e.getMessage());
        }

        // 2. 處理 application.yml 中的備用靜態清單 (排除已在 DB 處理過的業者)
        List<SyncConfig.CpoApiConfig> sources = syncConfig.getCpoSources();
        if (sources != null && !sources.isEmpty()) {
            for (SyncConfig.CpoApiConfig cpo : sources) {
                if (!cpo.isEnabled() || (cpo.getVendorId() != null && processedVendorIds.contains(cpo.getVendorId().toUpperCase()))) {
                    continue;
                }
                try {
                    SyncResult r = syncFromCpoApi(cpo.getApiUrl(), cpo.getToken(), cpo.getVendorId(), startTime, endTime);
                    totalTx += r.getTotalRecords();
                    totalSynced += r.getSyncedRecords();
                    totalSkipped += r.getSkippedRecords();
                } catch (Exception e) {
                    log.error("Failed to sync YAML-configured CPO: {}", cpo.getVendorId(), e);
                }
            }
        }

        return SyncResult.success(totalTx, totalSynced, totalSkipped);
    }
}
