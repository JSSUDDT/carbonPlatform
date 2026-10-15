package com.carbon.api.service.impl;

import com.carbon.api.service.GeocodingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OpenCage Geocoding API 實作
 * 包含速率限制機制，確保每秒最多一個請求
 */
@Service
public class GeocodingServiceImpl implements GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingServiceImpl.class);
    
    private static final String OPENCAGE_API_URL = "https://api.opencagedata.com/geocode/v1/json";
    
    // 速率限制：最小請求間隔（毫秒），設定為 1100ms 以確保安全邊際
    private static final long MIN_REQUEST_INTERVAL_MS = 1100;
    
    // 上次 API 調用的時間戳記（線程安全）
    private static volatile long lastRequestTime = 0;
    
    // 用於同步的鎖對象
    private static final Object lock = new Object();
    
    @Value("${opencage.api.key:}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public GeocodingServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 速率限制：確保每次 API 請求之間至少間隔 MIN_REQUEST_INTERVAL_MS 毫秒
     */
    private void enforceRateLimit() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTime;
            
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                long waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                logger.info("OpenCage API rate limit: waiting {} ms before sending request", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for rate limit", e);
                }
            }
            
            lastRequestTime = System.currentTimeMillis();
        }
    }
    
    @Override
    public String getCountryCode(String latitude, String longitude) {
        // 驗證輸入參數
        if (latitude == null || longitude == null || 
            latitude.trim().isEmpty() || longitude.trim().isEmpty()) {
            logger.warn("Latitude/longitude parameters are empty, returning NA");
            return "NA";
        }
        
        // 驗證 API Key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OpenCage API Key is not configured, returning NA");
            return "NA";
        }
        
        // 記錄 API Key 資訊（只顯示前後各 4 個字符，中間用 *** 隱藏）
        String maskedKey = apiKey.length() > 8 ? 
                apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4) : 
                apiKey.substring(0, Math.min(8, apiKey.length())) + "***";
        logger.info("OpenCage API Key configured: {} (length: {})", maskedKey, apiKey.length());
        
        // 驗證 API Key 格式（OpenCage API key 通常是 32 個字符的十六進制字符串）
        if (apiKey.length() != 32) {
            logger.warn("OpenCage API Key length is {} (expected 32), but will try to use it anyway", apiKey.length());
        }
        
        try {
            // 解析經緯度為數字
            double lat = Double.parseDouble(latitude.trim());
            double lng = Double.parseDouble(longitude.trim());
            
            // 驗證經緯度範圍
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                logger.warn("Latitude/longitude out of valid range: lat={}, lng={}, returning NA", lat, lng);
                return "NA";
            }
            
            // 執行速率限制（確保每秒最多一個請求）
            enforceRateLimit();
            
            // 構建 API 請求 URL
            // OpenCage API 接受兩種格式：lat+lng 或 lat,lng
            // 使用逗號格式更可靠，避免 URL 編碼問題
            String query = String.format("%s,%s", lat, lng);
            // 只編碼特殊字符，保留逗號
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String url = String.format("%s?q=%s&key=%s&limit=1&no_annotations=1", 
                    OPENCAGE_API_URL, encodedQuery, apiKey);
            
            logger.info("Calling OpenCage API to get country code: lat={}, lng={}", lat, lng);
            
            // 驗證 URL 中是否包含 API key
            if (!url.contains("key=" + apiKey)) {
                logger.error("API Key is NOT included in the URL! URL: {}", url.replace(apiKey, "***"));
            } else {
                logger.info("API Key is included in URL (verified)");
            }
            
            // 記錄完整的 URL（隱藏 API key）
            String maskedUrl = url.replace(apiKey, "***MASKED***");
            logger.info("OpenCage API URL: {}", maskedUrl);
            logger.info("Query parameter (decoded): {}", query);
            
            // 驗證 URL 格式
            if (!url.startsWith(OPENCAGE_API_URL)) {
                logger.error("Invalid API URL format: {}", maskedUrl);
            }
            
            // 發送請求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            logger.info("HTTP Request sent successfully, status: {}", response.getStatusCode());
            logger.info("OpenCage API response status: {}", response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析 JSON 回應
                String responseBody = response.getBody();
                logger.info("OpenCage API response body length: {} characters", responseBody.length());
                logger.info("OpenCage API full response: {}", responseBody);
                
                // 檢查回應是否包含 results
                if (!responseBody.contains("\"results\"")) {
                    logger.error("OpenCage API response does NOT contain 'results' field!");
                    logger.error("This might indicate an error response or different format");
                } else {
                    logger.info("OpenCage API response contains 'results' field");
                }
                
                JsonNode rootNode = objectMapper.readTree(responseBody);
                
                // 檢查狀態
                JsonNode statusNode = rootNode.get("status");
                if (statusNode != null) {
                    JsonNode codeNode = statusNode.get("code");
                    if (codeNode != null) {
                        int code = codeNode.asInt();
                        if (code != 200) {
                            String message = statusNode.has("message") ? 
                                    statusNode.get("message").asText() : "Unknown error";
                            logger.error("OpenCage API returned error code: {}, message: {}, returning NA", code, message);
                            logger.info("Full API error response: {}", responseBody);
                            return "NA";
                        } else {
                            logger.info("OpenCage API status code: 200 (OK)");
                        }
                    } else {
                        logger.warn("OpenCage API response missing status.code field");
                    }
                } else {
                    logger.warn("OpenCage API response missing status field");
                }
                
                // 提取國家代碼
                JsonNode resultsNode = rootNode.get("results");
                if (resultsNode != null && resultsNode.isArray()) {
                    int resultsCount = resultsNode.size();
                    logger.info("OpenCage API returned {} results", resultsCount);
                    
                    if (resultsCount > 0) {
                        JsonNode firstResult = resultsNode.get(0);
                        logger.info("First result structure: {}", firstResult.toString());
                        
                        JsonNode componentsNode = firstResult.get("components");
                        
                        if (componentsNode != null) {
                            // 收集可用欄位名稱
                            StringBuilder fields = new StringBuilder();
                            componentsNode.fieldNames().forEachRemaining(field -> {
                                if (fields.length() > 0) fields.append(", ");
                                fields.append(field);
                            });
                            logger.info("Components node found, available fields: {}", 
                                    fields.length() > 0 ? fields.toString() : "none");
                            logger.info("Full components node: {}", componentsNode.toString());
                            
                            JsonNode countryCodeNode = componentsNode.get("country_code");
                            if (countryCodeNode != null) {
                                String countryCode = countryCodeNode.asText().toUpperCase();
                                logger.info("Successfully retrieved country code from OpenCage API: {} (lat={}, lng={})", 
                                        countryCode, lat, lng);
                                return countryCode;
                            } else {
                                logger.warn("OpenCage API response missing country_code in components. Available fields: {}", 
                                        fields.length() > 0 ? fields.toString() : "none");
                                logger.info("Full components node (missing country_code): {}", componentsNode.toString());
                            }
                        } else {
                            logger.warn("OpenCage API response missing components node in first result");
                            logger.info("First result structure (missing components): {}", firstResult.toString());
                        }
                    } else {
                        logger.warn("OpenCage API returned empty results array");
                    }
                } else {
                    logger.warn("OpenCage API response missing or invalid results field");
                }
                
                logger.error("Country code not found in OpenCage API response, returning NA");
                logger.info("Full API response for debugging: {}", responseBody);
                return "NA";
                
            } else {
                logger.error("OpenCage API request failed, HTTP status: {}, returning NA", 
                        response.getStatusCode());
                if (response.getBody() != null) {
                    logger.info("Error response body: {}", response.getBody());
                } else {
                    logger.warn("Error response body is null");
                }
                return "NA";
            }
            
        } catch (NumberFormatException e) {
            logger.error("Invalid latitude/longitude format: lat={}, lng={}, returning NA", latitude, longitude, e);
            return "NA";
        } catch (RestClientException e) {
            logger.error("Network error when calling OpenCage API, returning NA", e);
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
            return "NA";
        } catch (Exception e) {
            logger.error("Error processing OpenCage API response, returning NA", e);
            logger.debug("Exception details: {}", e.getMessage(), e);
            return "NA";
        }
    }
}

