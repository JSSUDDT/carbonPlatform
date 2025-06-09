package com.carbon.api.controller;

import com.carbon.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    public Map<String, String> generateTestToken(@RequestParam String vendorId) {
        Map<String, String> response = new HashMap<>();
        String token = jwtTokenProvider.generateToken(vendorId);
        response.put("token", token);
        return response;
    }
    
    @GetMapping("/generate-test-token")
    public Map<String, String> generateTestTokenGet(@RequestParam String vendorId) {
        return generateTestToken(vendorId);
    }
} 