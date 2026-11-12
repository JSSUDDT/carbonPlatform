package com.carbon.api.controller;

import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.repository.VendorRepository;
import com.carbon.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private VendorRepository vendorRepository;

    @GetMapping("/check-auth")
    public Map<String, Object> checkAuth() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        response.put("isAuthenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getPrincipal() : null);
        response.put("authorities", auth != null ? auth.getAuthorities() : null);
        
        return response;
    }
    
    @GetMapping("/validate-token")
    public Map<String, Object> validateToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isValid = jwtTokenProvider.validateToken(token);
        response.put("isValid", isValid);
        
        if (isValid) {
            String vendorId = jwtTokenProvider.getVendorIdFromToken(token);
            response.put("vendorId", vendorId);
            
            // 解析JWT payload
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                try {
                    String payload = new String(Base64.getDecoder().decode(parts[1]));
                    response.put("payload", payload);
                } catch (Exception e) {
                    response.put("error", "無法解析payload: " + e.getMessage());
                }
            }
        }
        
        return response;
    }
    
    @PostMapping("/generate-test-token")
    public Map<String, String> generateTestToken(
            @RequestParam String vendorId,
            @RequestParam(required = false) String role) {
        Map<String, String> response = new HashMap<>();
        String actualRole = role;
        if (!StringUtils.hasText(actualRole)) {
            CarbonVendor vendor = vendorRepository.findByVendorId(vendorId);
            if (vendor != null && StringUtils.hasText(vendor.getRole())) {
                actualRole = vendor.getRole();
            } else {
                actualRole = "ROLE_USER";
            }
        }
        String token = jwtTokenProvider.generateToken(vendorId, actualRole);
        response.put("token", token);
        return response;
    }
    
    @GetMapping("/generate-test-token")
    public Map<String, String> generateTestTokenGet(
            @RequestParam String vendorId,
            @RequestParam(required = false) String role) {
        return generateTestToken(vendorId, role);
    }

    /**
     * 模擬外部 CPO 業者端提供的單一數據查詢 API (GET /api/v1/carbon/dataset)
     * 供本機及 SIT 測試環境聯測與模擬業者行為
     */
    @GetMapping("/mock-cpo/dataset")
    public ResponseEntity<com.carbon.api.dto.external.cpo.CpoDatasetResponse> mockCpoDataset(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false, defaultValue = "MOCK_CPO_EV") String vendorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "1000") Integer pageSize) {

        // 模擬 Token 驗證 (Bearer mock-secret-token)
        boolean hasAuth = (authHeader != null && authHeader.contains("mock-secret-token"));
        
        com.carbon.api.dto.external.cpo.CpoStationDto st1 = com.carbon.api.dto.external.cpo.CpoStationDto.builder()
                .stationId("MOCK_STA_01")
                .stationName("台北信義旗艦快充站")
                .gpsLatitude("25.0339")
                .gpsLongitude("121.5645")
                .address("台北市信義區信義路五段7號")
                .build();

        com.carbon.api.dto.external.cpo.CpoChargerDto ch1 = com.carbon.api.dto.external.cpo.CpoChargerDto.builder()
                .stationId("MOCK_STA_01")
                .chargerId("MOCK_EVSE_01")
                .connectId("1")
                .firstEnableDate("2025-01-01")
                .chargerType("DC")
                .chargerKw("180")
                .build();

        com.carbon.api.dto.external.cpo.CpoChargerDto ch2 = com.carbon.api.dto.external.cpo.CpoChargerDto.builder()
                .stationId("MOCK_STA_01")
                .chargerId("MOCK_EVSE_01")
                .connectId("2")
                .firstEnableDate("2025-01-01")
                .chargerType("DC")
                .chargerKw("180")
                .build();

        // 模擬交易
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String t1Start = now.minusHours(2).format(dtf);
        String t1End = now.minusHours(1).minusMinutes(15).format(dtf);

        com.carbon.api.dto.external.cpo.CpoTransactionDto tx1 = com.carbon.api.dto.external.cpo.CpoTransactionDto.builder()
                .transNo("TX_MOCK_" + System.currentTimeMillis() + "_1")
                .stationId("MOCK_STA_01")
                .chargerId("MOCK_EVSE_01")
                .connectId("1")
                .startTime(t1Start)
                .endTime(t1End)
                .kwh(38.5)
                .carType("純電")
                .carNo("EAB-6688")
                .invoiceNo("AB12345678")
                .build();

        com.carbon.api.dto.external.cpo.CpoDatasetData data = com.carbon.api.dto.external.cpo.CpoDatasetData.builder()
                .stations(java.util.Collections.singletonList(st1))
                .chargers(java.util.Arrays.asList(ch1, ch2))
                .transactions(java.util.Collections.singletonList(tx1))
                .build();

        com.carbon.api.dto.external.cpo.CpoDatasetResponse response = com.carbon.api.dto.external.cpo.CpoDatasetResponse.builder()
                .resultCode("0000")
                .resultMsg(hasAuth ? "Success (Authenticated)" : "Success (Mock Open)")
                .vendorId(vendorId)
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
