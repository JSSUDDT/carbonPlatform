package com.carbon.api.service;

import com.carbon.api.dto.RegisterRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.TokenRequest;
import com.carbon.api.dto.TokenResponse;
import com.carbon.api.entity.CarbonVendor;
import java.util.List;

public interface VendorService {
    TokenResponse getToken(TokenRequest request);
    
    ResultResponse register(RegisterRequest request);
    
    CarbonVendor findByVendorId(String vendorId);
    
    List<CarbonVendor> findAll();
} 