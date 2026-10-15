# 台灣電動車充電系統碳權專案 (Verra 5455)
# 業者端資料庫手動存取 (Admin Manual DB Ingestion) 開發說明書

| 文件資訊 | 內容說明 |
| :--- | :--- |
| **文件名稱** | 業者端資料庫管理員手動存取系統開發規格說明書 (Admin Manual Ingestion Specification) |
| **專案名稱** | 台灣電動車充電系統碳權平台 (TEVCS) ｜ Verra Registered Project ID: 5455 |
| **文件版本** | v2.1 (架構整合修訂：支援雙軌並存——保留既有常態排程同步，擴充特定業者之受控手動拉取) |
| **文件狀態** | 正式釋出 (Ready for Implementation) |
| **適用對象** | 前端/後端開發工程師 (Spring Boot / Thymeleaf)、資料庫管理師 (DBA)、維運工程師 |
| **最後更新日期** | 2026-10-15 |

---

## 1. 架構核心原則：雙軌並存架構 (Dual-Track Hybrid Ingestion Architecture)

### 1.1 業務場景與雙軌分類 (Business Decision)
平台考量不同合作業者之資安政策與連線權限差異，採行**「自動排程」與「受控手動」雙軌並存**架構：

1. **軌道 A：常態自動排程同步 (Automated Scheduled Track - 既有機制維持不變)**
   * **適用對象**：充壩內部資料庫（如跨 Schema 之 `ccms.trans_mapping`）及已常態授權開放定時存取之合作 CPO 業者。
   * **運作機制**：保留現行 `DataSyncScheduler` 與 `DataSyncService`，由系統依設定之 Cron（如每 5~10 分鐘）於背景自動執行增量抽取與清洗入庫。
   * **原則**：既有生產流程完全不受影響，不更動既有配置與邏輯。

2. **軌道 B：特定受限業者受控手動拉取 (Admin On-Demand Manual Track - 本規格書擴充重點)**
   * **適用對象**：對外網連線管制嚴格之特定外部業者（例如特定公/私有停車場、特定獨立 CPO）。
   * **限制條件**：該類業者**不開放全天候或無人排程連入**，僅於雙方約定之特定離峰時段（例如「今晚 02:00 ~ 04:00」）暫時開放資料庫 View 與防火牆。
   * **運作機制**：
     - 系統管理員（`ROLE_ADMIN`）於約定時段登入後台。
     - 於管理員專屬介面選取指定業者，手動點擊 `[🚀 立即拉取業者資料庫]`。
     - 系統以短暫動態連線（逾時 15 秒）連入該業者之唯讀 SQL View，執行單次增量抽取、清洗與對帳入庫。
     - 抽取完成後連線立即釋放關閉，杜絕長時間連線佔用或無預警連線之疑慮。

3. **統一核心入庫清洗引擎 (Shared Ingestion & Anti-Tampering Engine)**
   * 兩種軌道獲取之交易數據，一律流入相同的防篡改入庫管線：
     - `findByTransNo` 防重檢核（交易已存在者嚴格略過，代碼 `0006`，不可覆蓋）。
     - 場站與槍頭存在性檢核（代碼 `0004`、`0007`）。
     - 時間合理性與度數清洗。

```
┌────────────────────────────────────────────────────────┐          ┌────────────────────────────────────────────────────────┐
│               業者端來源資料庫 (Data Sources)           │          │              充壩碳權平台 (Carbon Platform)            │
│                                                        │          │                                                        │
│  [軌道 A: 常態排程業者 (如 ccms.trans_mapping)]         │          │  [自動排程器 (DataSyncScheduler)]                       │
│    └── 既有跨 Schema 授權 ──────────────────────────────┼─────────►│     │ (保留既有 Cron 自動排程，每 5~10 分鐘)            │
│                                                        │          │     ▼                                                  │
│                                                        │          │  [共享入庫清洗引擎 (Core Ingestion Pipeline)]           │
│                                                        │          │     │ • findByTransNo 防重不可篡改 (代碼 0006)          │
│                                                        │          │     │ • 場站/槍頭關聯存在性驗證 (代碼 0004/0007)       │
│  [軌道 B: 特定受限業者 (約定離峰時段開放)]             │          │     │ • 寫入系統資料庫 (ccms_sit.carbon_charge)        │
│    ├── v_carbon_station   (場站 View)                  │          │     ▲                                                  │
│    ├── v_carbon_charger   (充電槍 View)                │          │     │ (單次拉取完成後連線立即釋放)                     │
│    └── v_carbon_charge_log(交易度數 View)              │ ◄─────── │  [多租戶動態連線池 (DynamicDataSourceService)]         │
│             ▲                                          │   SSL    │     ▲                                                  │
│             │ (唯讀 SELECT)                            │ 單次拉取 │     │                                                  │
│        [專用帳號 gcb_carbon_sync]                      │          │  [管理員介面 (Web Admin UI 手動按鈕觸發)]              │
│                                                        │          │     └── 僅 ROLE_ADMIN 於離峰約定時段點擊觸發           │
└────────────────────────────────────────────────────────┘          └────────────────────────────────────────────────────────┘
```

---

## 2. Web UI 與前端按鈕互動規格 (Frontend UI Specification)

### 2.1 權限控制原則
* 一般業者帳號（`ROLE_USER`）：**完全看不到**手動拉取按鈕與外部連線設定。
* 系統管理員帳號（`ROLE_ADMIN`）：登入後於導覽列或頁面可見專用同步操作面板。

### 2.2 UI 呈現位置與組件設計
在管理員後台的 `transactions.html`（或專用管理員頁面），於工具列右上角新增管理員專屬工具組：

```html
<!-- 僅 ROLE_ADMIN 可見的手動拉取按鈕組件 -->
<div class="btn-group mr-2" th:if="${#request.isUserInRole('ROLE_ADMIN')}">
    <button type="button" class="btn btn-sm btn-warning font-weight-bold" id="manualSyncBtn" data-toggle="modal" data-target="#manualSyncModal">
        <i class="fas fa-cloud-download-alt mr-1"></i><span>手動拉取外部數據</span>
    </button>
</div>
```

### 2.3 手動拉取操作彈窗 (Modal Interaction Flow)
1. **彈出視窗內容**：
   * **選擇目標業者 (Vendor Dropdown)**：下拉選單列出已配置資料庫連線之業者名稱與 `vendorId`。
   * **同步項目勾選 (Sync Scope)**：
     - [x] 充電站場域資料 (`v_carbon_station`)
     - [x] 充電設備與槍頭 (`v_carbon_charger`)
     - [x] 充電交易度數記錄 (`v_carbon_charge_log` - 增量讀取)
   * **安全防護確認勾選**：`[ ] 我已確認業者已於目前時段開放防火牆與資料庫存取`。
   * **執行按鈕**：`[🚀 開始執行拉取]`（點擊後鎖定按鈕，顯示 Loading Spinner）。
2. **即時執行回饋視窗 (Execution Result Display)**：
   * 連線中狀態提示：`正在連線至 [台北停車場] 外部資料庫 (52.xxx.xxx.xxx)...`
   * 成功完成提示卡片：
     - ✅ **連線耗時**：`1,240 ms`
     - 📥 **抓取交易筆數**：`152 筆`
     - 💾 **成功新增入庫**：`150 筆`（累計有效充電度數：`3,450.50 kWh`）
     - ⚠️ **重複略過筆數 (0006)**：`2 筆`（交易單號已存在，不可覆蓋）
     - ❌ **資料異常略過**：`0 筆`
     - 🕒 **最新水位線時間**：`2026-10-14 15:30:00`

---

## 3. 後端 API 與控制器規格 (REST Controller Specification)

### 3.1 擴充現有 `DataSyncController.java`
將原先無參數的 `/api/sync/manual` 升級為指定業者與嚴格權限保護的端點：

```java
package com.carbon.api.controller;

import com.carbon.api.dto.ManualSyncRequest;
import com.carbon.api.dto.SyncResult;
import com.carbon.api.service.ScheduledDbIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@PreAuthorize("hasRole('ROLE_ADMIN')") // 嚴格限制：僅系統管理員可呼叫！
public class AdminDataSyncController {

    @Autowired
    private ScheduledDbIngestionService ingestionService;

    /**
     * 管理員手動觸發單一業者資料庫拉取
     * POST /api/admin/sync/vendor
     */
    @PostMapping("/vendor")
    public ResponseEntity<SyncResult> triggerManualVendorSync(
            @Valid @RequestBody ManualSyncRequest request,
            Authentication authentication) {
        
        String adminUsername = authentication != null ? authentication.getName() : "ADMIN_USER";
        log.info("[AdminSync] Admin '{}' manually triggered DB pull for vendor: {}", 
                adminUsername, request.getVendorId());

        // 呼叫底層手動抽取服務
        SyncResult result = ingestionService.executeManualIngestion(
                request.getVendorId(), 
                adminUsername, 
                request.isSyncStations(), 
                request.isSyncChargers()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 測試外部業者資料庫連線狀況 (Ping/Check Connection)
     * GET /api/admin/sync/test-connection/{vendorId}
     */
    @GetMapping("/test-connection/{vendorId}")
    public ResponseEntity<String> testConnection(@PathVariable String vendorId) {
        boolean connected = ingestionService.testVendorConnection(vendorId);
        if (connected) {
            return ResponseEntity.ok("外部資料庫連線測試成功！");
        } else {
            return ResponseEntity.badRequest().body("連線失敗：請確認業者是否已開啟防火牆或授權帳密正確。");
        }
    }
}
```

### 3.2 請求與回應 DTO 定義
```java
package com.carbon.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ManualSyncRequest {
    @NotBlank(message = "業者ID不得為空")
    private String vendorId;

    private boolean syncStations = true;  // 是否同步場站
    private boolean syncChargers = true;  // 是否同步槍頭
}
```

---

## 4. 本地資料庫 DDL 調整 (Schema Adjustments)

移除非必要之 Cron 欄位，增加管理員審核軌跡欄位：

```sql
-- 1. 業者外部資料庫連線配置表 (支援雙軌模式：SCHEDULED 或 MANUAL_ONLY)
CREATE TABLE IF NOT EXISTS `carbon_vendor_db_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵',
  `vendor_id` VARCHAR(50) NOT NULL UNIQUE COMMENT '業者ID (關聯 carbon_vendor)',
  `sync_mode` VARCHAR(20) NOT NULL DEFAULT 'MANUAL_ONLY' COMMENT '同步模式: SCHEDULED(自動排程), MANUAL_ONLY(管理員手動), DISABLED(停用)',
  `cron_expression` VARCHAR(50) DEFAULT NULL COMMENT '自訂排程表達式 (若為 SCHEDULED 且需個別排程時使用)',
  `db_type` VARCHAR(20) NOT NULL DEFAULT 'MYSQL' COMMENT '資料庫類型: MYSQL, POSTGRESQL, MSSQL, ORACLE',
  `host` VARCHAR(100) NOT NULL COMMENT '主機位置/IP',
  `port` INT NOT NULL DEFAULT 3306 COMMENT '端口',
  `db_name` VARCHAR(100) NOT NULL COMMENT '資料庫名稱',
  `username` VARCHAR(100) NOT NULL COMMENT '連線帳號 (如 gcb_carbon_sync)',
  `password_encrypted` VARCHAR(255) NOT NULL COMMENT 'AES-256 加密密碼',
  `use_ssl` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用 SSL 加密',
  `connection_timeout_ms` INT NOT NULL DEFAULT 15000 COMMENT '連線超時(毫秒)',
  `query_timeout_sec` INT NOT NULL DEFAULT 120 COMMENT '查詢超時(秒)',
  `last_manual_sync_time` DATETIME DEFAULT NULL COMMENT '最後一次手動拉取時間',
  `last_sync_status` VARCHAR(20) DEFAULT NULL COMMENT '最後一次狀態 (SUCCESS/FAILED)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='業者外部資料庫連線參數表';

-- 2. 增量水位線標記表
CREATE TABLE IF NOT EXISTS `carbon_sync_watermark` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `vendor_id` VARCHAR(50) NOT NULL,
  `view_name` VARCHAR(50) NOT NULL COMMENT '檢視表名稱: v_carbon_charge_log',
  `watermark_time` DATETIME NOT NULL COMMENT '最後一次成功抽取之交易時間',
  `last_trans_no` VARCHAR(100) DEFAULT NULL,
  `last_sync_count` INT NOT NULL DEFAULT 0,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_vendor_view` (`vendor_id`, `view_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='數據同步高水位線記錄表';

-- 3. 手動拉取執行日誌表 (增加 admin_username 稽核欄位)
CREATE TABLE IF NOT EXISTS `carbon_sync_execution_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `job_id` VARCHAR(64) NOT NULL COMMENT '批次唯一UUID',
  `vendor_id` VARCHAR(50) NOT NULL,
  `admin_username` VARCHAR(50) NOT NULL COMMENT '觸發執行的管理員帳號 (Audit Trail)',
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL COMMENT 'RUNNING, SUCCESS, FAILED, PARTIAL',
  `fetched_count` INT NOT NULL DEFAULT 0,
  `inserted_count` INT NOT NULL DEFAULT 0,
  `skipped_duplicate_count` INT NOT NULL DEFAULT 0 COMMENT '略過重複筆數(0006)',
  `skipped_invalid_count` INT NOT NULL DEFAULT 0,
  `error_message` TEXT DEFAULT NULL,
  INDEX `idx_vendor_admin` (`vendor_id`, `admin_username`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理員手動拉取執行稽核紀錄表';
```

---

## 5. 後端核心手動抽取服務實作 (`ScheduledDbIngestionService.java`)

```java
package com.carbon.api.service.impl;

import com.carbon.api.dto.SyncResult;
import com.carbon.api.entity.*;
import com.carbon.api.repository.*;
import com.carbon.api.service.DynamicDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ScheduledDbIngestionService {

    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;

    @Autowired
    private VendorDbConfigRepository configRepository;

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerRepository chargerRepository;

    @Autowired
    private SyncWatermarkRepository watermarkRepository;

    @Autowired
    private SyncExecutionLogRepository logRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 執行管理員手動觸發之單次資料庫拉取
     */
    public SyncResult executeManualIngestion(String vendorId, String adminUser, boolean syncStations, boolean syncChargers) {
        CarbonVendorDbConfig config = configRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("未找到業者 [" + vendorId + "] 之外部資料庫連線配置！"));

        String jobId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        log.info("[AdminJob:{}] Executing manual DB pull for vendor '{}' by admin '{}'", jobId, vendorId, adminUser);

        CarbonSyncExecutionLog execLog = new CarbonSyncExecutionLog();
        execLog.setJobId(jobId);
        execLog.setVendorId(vendorId);
        execLog.setAdminUsername(adminUser);
        execLog.setStartTime(startTime);
        execLog.setStatus("RUNNING");
        logRepository.save(execLog);

        try {
            JdbcTemplate vendorJdbc = dynamicDataSourceService.getJdbcTemplate(config);

            // 1. 選填：同步場站與充電槍基本資料
            if (syncStations) {
                syncStations(vendorId, vendorJdbc);
            }
            if (syncChargers) {
                syncChargers(vendorId, vendorJdbc);
            }

            // 2. 核心：增量拉取充電交易度數資料 (以水位線增量過濾)
            IngestionStats stats = pullTransactionsWithWatermark(vendorId, vendorJdbc);

            long duration = System.currentTimeMillis() - startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            execLog.setEndTime(LocalDateTime.now());
            execLog.setDurationMs(duration);
            execLog.setStatus("SUCCESS");
            execLog.setFetchedCount(stats.fetched);
            execLog.setInsertedCount(stats.inserted);
            execLog.setSkippedDuplicateCount(stats.skippedDuplicates);
            execLog.setSkippedInvalidCount(stats.skippedInvalid);
            logRepository.save(execLog);

            // 更新業者配置表最後同步時間
            config.setLastManualSyncTime(LocalDateTime.now());
            config.setLastSyncStatus("SUCCESS");
            configRepository.save(config);

            return SyncResult.success(stats.fetched, stats.inserted, stats.skippedDuplicates);

        } catch (Exception e) {
            log.error("[AdminJob:" + jobId + "] Execution failed", e);
            execLog.setEndTime(LocalDateTime.now());
            execLog.setStatus("FAILED");
            execLog.setErrorMessage(e.getMessage());
            logRepository.save(execLog);

            config.setLastManualSyncTime(LocalDateTime.now());
            config.setLastSyncStatus("FAILED");
            configRepository.save(config);

            return SyncResult.failure("手動同步失敗: " + e.getMessage(), e.toString());
        }
    }

    /**
     * 測試外部連線
     */
    public boolean testVendorConnection(String vendorId) {
        try {
            CarbonVendorDbConfig config = configRepository.findByVendorId(vendorId)
                    .orElseThrow(() -> new IllegalArgumentException("業者配置不存在"));
            JdbcTemplate vendorJdbc = dynamicDataSourceService.getJdbcTemplate(config);
            Integer testResult = vendorJdbc.queryForObject("SELECT 1", Integer.class);
            return testResult != null && testResult == 1;
        } catch (Exception e) {
            log.warn("Test connection failed for vendor: " + vendorId, e);
            return false;
        }
    }

    private IngestionStats pullTransactionsWithWatermark(String vendorId, JdbcTemplate vendorJdbc) {
        IngestionStats stats = new IngestionStats();

        // 讀取增量水位線 (若無則取 7 天前)
        CarbonSyncWatermark wm = watermarkRepository.findByVendorIdAndViewName(vendorId, "v_carbon_charge_log")
                .orElseGet(() -> {
                    CarbonSyncWatermark newWm = new CarbonSyncWatermark();
                    newWm.setVendorId(vendorId);
                    newWm.setViewName("v_carbon_charge_log");
                    newWm.setWatermarkTime(LocalDateTime.now().minusDays(7));
                    return newWm;
                });

        String sql = "SELECT transNo, stationId, chargerId, connectId, startTime, endTime, kwh, " +
                     "invoiceNo, invoiceDate, customerId, carType, carTonnage, carNo " +
                     "FROM v_carbon_charge_log " +
                     "WHERE startTime > ? " +
                     "ORDER BY startTime ASC LIMIT 5000";

        List<Map<String, Object>> rows = vendorJdbc.queryForList(sql, wm.getWatermarkTime().format(DT_FMT));
        stats.fetched = rows.size();

        LocalDateTime latestTimestamp = wm.getWatermarkTime();
        String latestTransNo = wm.getLastTransNo();

        for (Map<String, Object> row : rows) {
            String transNo = (String) row.get("transNo");
            String kwhStr = String.valueOf(row.get("kwh"));
            String startStr = (String) row.get("startTime");
            String endStr = (String) row.get("endTime");

            // 1. 必填校驗
            if (transNo == null || transNo.trim().isEmpty() || startStr == null || endStr == null) {
                stats.skippedInvalid++;
                continue;
            }

            // 2. 度數校驗 (kWh > 0 且合理上限)
            try {
                double kwh = Double.parseDouble(kwhStr);
                if (kwh <= 0.0 || kwh > 500.0) {
                    stats.skippedInvalid++;
                    continue;
                }
            } catch (Exception ex) {
                stats.skippedInvalid++;
                continue;
            }

            // 3. 不可篡改與冪等性防線 (transNo 已存在則略過)
            if (chargeRepository.findByTransNo(transNo) != null) {
                stats.skippedDuplicates++;
                continue;
            }

            // 4. 入庫保存
            CarbonCharge charge = new CarbonCharge();
            charge.setTransNo(transNo);
            charge.setVendorId(vendorId);
            charge.setStationId((String) row.get("stationId"));
            charge.setChargerId((String) row.get("chargerId"));
            charge.setConnectId((String) row.get("connectId"));
            charge.setStartTime(startStr);
            charge.setEndTime(endStr);
            charge.setKwh(kwhStr);
            charge.setCarType((String) row.get("carType"));
            charge.setCarNo((String) row.get("carNo"));
            charge.setInvoiceNo((String) row.get("invoiceNo"));
            charge.setInvoiceDate((String) row.get("invoiceDate"));
            charge.setCustomerId((String) row.get("customerId"));
            charge.setCarbonCredit("1");
            charge.setCreateTime(LocalDateTime.now().format(DT_FMT));
            charge.setUpdateTime(LocalDateTime.now().format(DT_FMT));

            chargeRepository.save(charge);
            stats.inserted++;

            LocalDateTime rowTime = LocalDateTime.parse(startStr, DT_FMT);
            if (rowTime.isAfter(latestTimestamp)) {
                latestTimestamp = rowTime;
                latestTransNo = transNo;
            }
        }

        // 更新水位線
        wm.setWatermarkTime(latestTimestamp);
        wm.setLastTransNo(latestTransNo);
        wm.setLastSyncCount(stats.inserted);
        watermarkRepository.save(wm);

        return stats;
    }

    private void syncStations(String vendorId, JdbcTemplate vendorJdbc) {
        // 同步場站資訊邏輯
    }

    private void syncChargers(String vendorId, JdbcTemplate vendorJdbc) {
        // 同步充電槍資訊邏輯 (檢驗 firstEnableDate)
    }

    private static class IngestionStats {
        int fetched = 0;
        int inserted = 0;
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
    }
}
```

---

## 6. 開發任務清單 (Developer Checklist)

1. [ ] **DB 調整**：執行 DDL 建立 `carbon_vendor_db_config`、`carbon_sync_watermark`、`carbon_sync_execution_log`。
2. [ ] **後端 API 實作**：在 `AdminDataSyncController` 實作 `POST /api/admin/sync/vendor` 與 `GET /api/admin/sync/test-connection/{vendorId}`。
3. [ ] **權限限制驗證**：確保僅具備 `ROLE_ADMIN` 的 JWT Token 可成功調用，一般業者帳號呼叫回傳 `403 Forbidden`。
4. [ ] **前端按鈕與 Modal**：在 `transactions.html` 整合「手動拉取外部數據」按鈕與結果彈窗，僅管理員可見。
5. [ ] **測試與驗證**：
   - 模擬業者開放連線，管理員點擊按鈕，確認交易度數入庫成功。
   - 再次點擊按鈕，確認重複單號略過（`skippedDuplicateCount > 0`，`insertedCount = 0`）。
   - 關閉外部資料庫模擬連線失敗，確認 15 秒內超時拋出錯誤提示，主系統運行正常無凍結。
