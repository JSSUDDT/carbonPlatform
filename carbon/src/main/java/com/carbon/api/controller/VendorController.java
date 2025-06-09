package com.carbon.api.controller;

import com.carbon.api.dto.*;
import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    private static final Logger logger = LoggerFactory.getLogger(VendorController.class);

    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private StationService stationService;
    
    @Autowired
    private ChargerService chargerService;
    
    @Autowired
    private StationPowerService stationPowerService;
    
    @Autowired
    private ChargeService chargeService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @PostMapping("/GetToken")
    public ResponseEntity<TokenResponse> getToken(@Valid @RequestBody TokenRequest request) {
        logger.debug("Received request to get token, vendorId: {}", request.getVendorId());
        return ResponseEntity.ok(vendorService.getToken(request));
    }
    
    @PostMapping("/Register")
    public ResponseEntity<ResultResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.debug("Received request to register, vendorId: {}", request.getVendorId());
        return ResponseEntity.ok(vendorService.register(request));
    }
    
    /**
     * 獲取充電站信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電站信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電站信息
     * 3. 支持按關鍵字搜索和狀態篩選
     */
    @GetMapping("/StationInfo")
    public ResponseEntity<?> getStationInfo(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        logger.debug("Received request to get station information, search: {}, status: {}", search, status);
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電站
        if (!isAdmin && (vendorId != null && !authorizationService.hasAccessToVendor(vendorId))) {
            logger.warn("User has no access to this vendor resource: {}", vendorId);
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        // 獲取當前用戶ID（如果是普通用戶且未指定vendorId）
        if (!isAdmin && vendorId == null) {
            vendorId = authorizationService.getCurrentVendorId();
        }
        
        // 如果沒有提供搜索條件和狀態過濾，則使用基本查詢方法
        if ((search == null || search.isEmpty()) && (status == null || status.isEmpty())) {
            return ResponseEntity.ok(stationService.getStationInfo(vendorId));
        }
        
        // 否則使用帶有搜索和狀態過濾的方法
        return ResponseEntity.ok(stationService.getStationInfo(vendorId, search, status));
    }
    
    /**
     * 獲取充電樁信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電樁信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電樁信息
     * 3. 支持按關鍵字搜索和狀態篩選
     */
    @GetMapping("/ChargerInfo")
    public ResponseEntity<?> getChargerInfo(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        logger.debug("Received request to get charger information, search: {}, status: {}", search, status);
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電樁
        if (!isAdmin && (vendorId != null && !authorizationService.hasAccessToVendor(vendorId))) {
            logger.warn("User has no access to this vendor resource: {}", vendorId);
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        // 獲取當前用戶ID（如果是普通用戶且未指定vendorId）
        if (!isAdmin && vendorId == null) {
            vendorId = authorizationService.getCurrentVendorId();
        }
        
        // 如果沒有提供搜索條件和狀態過濾，則使用基本查詢方法
        if ((search == null || search.isEmpty()) && (status == null || status.isEmpty())) {
            return ResponseEntity.ok(chargerService.getChargerInfo(vendorId, stationId));
        }
        
        // 否則使用帶有搜索和狀態過濾的方法
        return ResponseEntity.ok(chargerService.getChargerInfo(vendorId, stationId, search, status));
    }
    
    /**
     * 根據充電樁ID獲取充電樁信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電樁信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電樁信息
     * 3. 返回所有匹配chargerId的充電樁及其連接頭
     */
    @GetMapping("/ChargerInfo/{chargerId}")
    public ResponseEntity<?> getChargerInfoById(@PathVariable String chargerId) {
        logger.debug("Received request to get charger information by ID: {}", chargerId);
        
        List<CarbonCharger> chargers = chargerService.getChargerInfoById(chargerId);
        
        if (chargers == null || chargers.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電樁
        // 篩選出用戶有權訪問的充電樁
        if (!isAdmin) {
            String currentVendorId = authorizationService.getCurrentVendorId();
            chargers = chargers.stream()
                    .filter(charger -> charger.getVendorId().equals(currentVendorId))
                    .collect(java.util.stream.Collectors.toList());
            
            if (chargers.isEmpty()) {
                logger.warn("User has no access to chargers with ID: {}", chargerId);
                throw new AccessDeniedException("No permission to access these charger resources");
            }
        }
        
        return ResponseEntity.ok(chargers);
    }
    
    /**
     * 根據充電樁ID和連接頭ID獲取單一充電樁信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電樁信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電樁信息
     */
    @GetMapping("/ChargerInfo/{chargerId}/{connectId}")
    public ResponseEntity<?> getChargerInfoByIdAndConnectId(@PathVariable String chargerId, @PathVariable String connectId) {
        logger.debug("Received request to get charger information by ID and connectId: {}, {}", chargerId, connectId);
        
        CarbonCharger charger = chargerService.getChargerInfoByIdAndConnectId(chargerId, connectId);
        
        if (charger == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電樁
        if (!isAdmin && !authorizationService.hasAccessToVendor(charger.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", charger.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(charger);
    }
    
    /**
     * 獲取場站負載信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有場站負載信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的場站負載信息
     */
    @GetMapping("/StationPower")
    public ResponseEntity<?> getStationPower(@RequestParam(required = false) String vendorId,
                                           @RequestParam(required = false) String stationId) {
        logger.debug("Received request to get station power information");
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的場站負載
        if (!isAdmin && (vendorId != null && !authorizationService.hasAccessToVendor(vendorId))) {
            logger.warn("User has no access to this vendor resource: {}", vendorId);
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        // 獲取當前用戶ID（如果是普通用戶且未指定vendorId）
        if (!isAdmin && vendorId == null) {
            vendorId = authorizationService.getCurrentVendorId();
        }
        
        return ResponseEntity.ok(stationPowerService.getStationPower(vendorId, stationId));
    }
    
    /**
     * 獲取充電交易信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電交易信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電交易信息
     * 3. 支持按照startTime進行時間段過濾
     */
    @GetMapping("/ChargeInfo")
    public ResponseEntity<?> getChargeInfo(@RequestParam(required = false) String vendorId,
                                         @RequestParam(required = false) String stationId,
                                         @RequestParam(required = false) String chargerId,
                                         @RequestParam(required = false) String startTimeFrom,
                                         @RequestParam(required = false) String startTimeTo) {
        logger.debug("Received request to get charge information with all params - vendorId: {}, stationId: {}, chargerId: {}, startTimeFrom: {}, startTimeTo: {}", 
            vendorId, stationId, chargerId, startTimeFrom, startTimeTo);
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電交易
        if (!isAdmin && (vendorId != null && !authorizationService.hasAccessToVendor(vendorId))) {
            logger.warn("User has no access to this vendor resource: {}", vendorId);
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        // 獲取當前用戶ID（如果是普通用戶且未指定vendorId）
        if (!isAdmin && vendorId == null) {
            vendorId = authorizationService.getCurrentVendorId();
        }
        
        // 使用新的服务方法，直接在数据库中过滤
        List<CarbonCharge> charges = chargeService.getChargeInfoWithTimeRange(
                vendorId, stationId, chargerId, startTimeFrom, startTimeTo);
        
        logger.debug("Total charges retrieved: {}", charges.size());
        
        return ResponseEntity.ok(charges);
    }
    
    /**
     * 根據交易編號獲取單筆充電交易信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電交易信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電交易信息
     */
    @GetMapping("/ChargeInfo/transNo/{transNo}")
    public ResponseEntity<?> getChargeInfoByTransNo(@PathVariable String transNo) {
        logger.debug("Received request to get charge information by transNo: {}", transNo);
        
        CarbonCharge charge = chargeService.getChargeInfoByTransNo(transNo);
        
        if (charge == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電交易
        if (!isAdmin && !authorizationService.hasAccessToVendor(charge.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", charge.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(charge);
    }
    
    /**
     * 更新場站信息
     * 
     * 說明：
     * 1. 用於創建或更新場站基本信息
     * 2. 場站必須先使用此接口創建，才能使用其他相關接口（如StationPower）
     * 3. admin權限可以新增/更新/刪除所有venderId的充電站
     * 4. user權限只能新增/更新/刪除自己venderId所屬的充電站
     */
    @PostMapping("/StationInfo")
    public ResponseEntity<ResultResponse> updateStationInfo(@Valid @RequestBody StationRequest request) {
        logger.debug("Received request to update station information, vendorId: {}", request.getVendorId());
        
        // 檢查權限
        if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", request.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(stationService.updateStationInfo(request));
    }
    
    /**
     * 更新充電樁信息
     * 
     * 說明：
     * 1. admin權限可以新增/更新/刪除所有venderId的充電樁，且充電站必須已存在
     * 2. user權限只能新增/更新/刪除自己venderId所屬的充電樁，且充電站必須已存在
     * 3. 每個充電樁可以有1~4個充電槍頭(connectId)
     * 4. 充電槍頭(connectId)透過connectorAry字段提供
     * 
     * 錯誤代碼：
     * - 0001: 請求數據無效
     * - 0004: 場站不存在
     */
    @PostMapping("/ChargerInfo")
    public ResponseEntity<ResultResponse> updateChargerInfo(@Valid @RequestBody ChargerRequest request) {
        logger.debug("Received request to update charger information, vendorId: {}, stationId: {}", request.getVendorId(), request.getStationId());
        
        // 檢查權限
        if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", request.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(chargerService.updateChargerInfo(request));
    }
    
    /**
     * 更新場站負載信息
     * 
     * 說明：
     * 1. 對於每個vendorId和stationId組合，同一個日期(dateInfo)的記錄只允許新增一次，不允許更新
     * 2. 如果請求中包含已存在的日期記錄，將返回錯誤信息
     * 3. 如果請求中包含重複的日期記錄，將返回錯誤信息
     * 4. 使用此接口前，必須先使用StationInfo接口創建相應的場站
     * 5. admin權限可以上傳所有venderId的充電站資料，且充電站必須已存在
     * 6. user權限只能上傳自己venderId所屬的充電站資料，且充電站必須已存在
     */
    @PostMapping("/StationPower")
    public ResponseEntity<ResultResponse> updateStationPower(@Valid @RequestBody StationPowerRequest request) {
        logger.debug("Received request to update station power information, vendorId: {}, stationId: {}", request.getVendorId(), request.getStationId());
        
        // 檢查權限
        if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", request.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(stationPowerService.updateStationPower(request));
    }
    
    /**
     * 上傳充電記錄信息
     * 
     * 說明：
     * 1. admin權限可以上傳所有venderId的充電站充電記錄資料，且充電站與充電樁必須已存在
     * 2. user權限只能上傳自己venderId所屬的充電站充電記錄資料，且充電站與充電樁必須已存在
     * 3. 同一個transNo的交易記錄只允許新增一次，不允許更新
     * 4. 如果請求中包含已存在的交易記錄，將返回錯誤信息
     * 5. 充電槍頭(connectId)必須存在於對應的充電樁(chargerId)中
     * 
     * 錯誤代碼：
     * - 0004: 場站不存在
     * - 0005: 充電樁不存在
     * - 0006: 交易記錄已存在，不允許更新
     * - 0007: 充電槍頭不存在
     */
    @PostMapping("/ChargeInfo")
    public ResponseEntity<ResultResponse> updateChargeInfo(@Valid @RequestBody ChargeRequest request) {
        logger.debug("Received request to update charge information, vendorId: {}", request.getVendorId());
        
        // 檢查權限
        if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", request.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(chargeService.updateChargeInfo(request));
    }
    
    /**
     * 根據充電站ID獲取單一充電站信息
     * 
     * 說明：
     * 1. ROLE_ADMIN可以獲取所有充電站信息
     * 2. ROLE_USER只能獲取自己vendorId所屬的充電站信息
     */
    @GetMapping("/StationInfo/{stationId}")
    public ResponseEntity<?> getStationInfoById(@PathVariable String stationId) {
        logger.debug("Received request to get station information by ID: {}", stationId);
        
        CarbonStation station = stationService.getStationInfoById(stationId);
        
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        
        // 如果是普通用戶，則只能查看自己的充電站
        if (!isAdmin && !authorizationService.hasAccessToVendor(station.getVendorId())) {
            logger.warn("User has no access to this vendor resource: {}", station.getVendorId());
            throw new AccessDeniedException("No permission to access this vendor resource");
        }
        
        return ResponseEntity.ok(station);
    }
    
    /**
     * 根據vendorId获取用户角色信息
     * 此接口被认证后的用户调用，用于获取自己的角色信息
     */
    @GetMapping("/UserRole")
    public ResponseEntity<?> getUserRole() {
        try {
            // 从安全上下文获取当前登录的vendorId
            String vendorId = authorizationService.getCurrentVendorId();
            if (vendorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ResultResponse("0001", "未授权的访问"));
            }
            
            // 根据vendorId查询用户信息
            CarbonVendor vendor = vendorService.findByVendorId(vendorId);
            if (vendor == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResultResponse("0002", "未找到用户信息"));
            }
            
            // 返回用户角色信息
            return ResponseEntity.ok(new ResultResponse("0000", vendor.getRole()));
        } catch (Exception e) {
            logger.error("获取用户角色失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResultResponse("9999", "系统错误"));
        }
    }
    
    /**
     * 獲取廠商列表
     * 
     * 說明：
     * 1. 只有 ROLE_ADMIN 可以獲取所有廠商列表
     * 2. 返回所有廠商的基本信息
     */
    @GetMapping("/VendorInfo")
    public ResponseEntity<?> getVendorInfo() {
        logger.debug("Received request to get vendor information");
        
        // 檢查是否有管理員權限
        boolean isAdmin = authorizationService.isCurrentUserAdmin();
        if (!isAdmin) {
            logger.warn("Non-admin user attempted to access vendor list");
            throw new AccessDeniedException("No permission to access vendor list");
        }
        
        // 獲取所有廠商信息
        List<CarbonVendor> vendors = vendorService.findAll();
        return ResponseEntity.ok(vendors);
    }
    
    /**
     * 處理參數錯誤請求路徑
     */
    @RequestMapping("/**")
    public ResponseEntity<ResultResponse> handleInvalidPath() {
        logger.warn("Received invalid API path request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "Invalid API path"));
    }
    
    /**
     * 處理訪問被拒絕的情況
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultResponse> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ResultResponse("0003", "No permission to access the requested resource"));
    }

    /**
     * 公開的充電站信息API，供地圖頁面使用
     * 注意：此接口不需要認證，可被公開訪問，但僅返回已啟用的充電站
     * 如果用戶已登錄，則根據角色返回不同範圍的充電站數據：
     * 1. ROLE_ADMIN可以看到全部充電站
     * 2. ROLE_USER只能看到自己的充電站
     */
    @GetMapping("/public/StationInfo")
    public ResponseEntity<?> getPublicStationInfo(
            @RequestParam(required = false) String search) {
        
        logger.debug("Received public request to get station information, search: {}", search);
        
        // 檢查用戶是否已登錄
        try {
            boolean isAdmin = authorizationService.isCurrentUserAdmin();
            String currentVendorId = authorizationService.getCurrentVendorId();
            
            // 如果是管理員，返回所有啟用的充電站
            if (isAdmin) {
                logger.debug("Admin user accessing station map, returning all stations");
                return ResponseEntity.ok(stationService.getStationInfo(null, search, "1"));
            } 
            // 如果是普通用戶，只返回該用戶的充電站
            else if (currentVendorId != null) {
                logger.debug("Regular user accessing station map, returning only their stations, vendorId: {}", currentVendorId);
                return ResponseEntity.ok(stationService.getStationInfo(currentVendorId, search, "1"));
            }
        } catch (Exception e) {
            // 如果獲取當前用戶信息時出錯（未登錄），則返回所有啟用的充電站
            logger.debug("No authenticated user or error getting user info, returning all enabled stations");
            return ResponseEntity.ok(stationService.getStationInfo(null, search, "1"));
        }
        
        // 默認情況（未登錄），返回所有啟用的充電站
        logger.debug("No authenticated user, returning all enabled stations");
        return ResponseEntity.ok(stationService.getStationInfo(null, search, "1"));
    }
} 