package com.carbon.api.service.impl;

import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.repository.VendorRepository;
import com.carbon.api.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);
    
    @Autowired
    private VendorRepository vendorRepository;

    @Override
    public boolean hasAccessToVendor(String vendorId) {
        // 檢查當前用戶是否能訪問該vendorId的資源
        if (isCurrentUserAdmin()) {
            logger.debug("Admin privileges, can access all vendor resources");
            return true;
        }
        
        String currentVendorId = getCurrentVendorId();
        boolean hasAccess = currentVendorId != null && currentVendorId.equals(vendorId);
        logger.debug("User {} attempting to access vendor {} resources, authorization result: {}", currentVendorId, vendorId, hasAccess);
        return hasAccess;
    }

    @Override
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        
        logger.debug("Checking if user {} is an admin, result: {}", authentication.getName(), isAdmin);
        return isAdmin;
    }

    @Override
    public String getCurrentVendorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        
        String vendorId = authentication.getName();
        logger.debug("Getting current logged-in vendor ID: {}", vendorId);
        return vendorId;
    }
} 