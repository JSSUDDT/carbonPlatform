package com.carbon.api.service.impl;

import com.carbon.api.dto.RegisterRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.TokenRequest;
import com.carbon.api.dto.TokenResponse;
import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.repository.VendorRepository;
import com.carbon.api.security.JwtTokenProvider;
import com.carbon.api.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class VendorServiceImpl implements VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TokenResponse getToken(TokenRequest request) {
        CarbonVendor vendor = vendorRepository.findByVendorId(request.getVendorId());
        
        if (vendor != null && passwordEncoder.matches(request.getPassword(), vendor.getPassword())) {
            String token = jwtTokenProvider.generateToken(vendor.getVendorId(), vendor.getRole());
            return new TokenResponse(token, "0000");
        }
        
        return new TokenResponse(null, "0001");
    }
    
    @Override
    @Transactional
    public ResultResponse register(RegisterRequest request) {
        try {
            // 檢查廠商ID是否已存在
            if (vendorRepository.findByVendorId(request.getVendorId()) != null) {
                return new ResultResponse("0002", "廠商ID已存在");
            }
            
            // 創建新廠商
            CarbonVendor vendor = new CarbonVendor();
            vendor.setVendorId(request.getVendorId());
            // 對密碼進行BCrypt加密
            vendor.setPassword(passwordEncoder.encode(request.getPassword()));
            vendor.setStatus(request.getStatus());
            // 設置角色
            vendor.setRole(request.getRole());
            vendor.setCreateTime(LocalDateTime.now().format(formatter));
            vendor.setUpdateTime(LocalDateTime.now().format(formatter));
            
            // 保存到數據庫
            vendorRepository.save(vendor);
            
            return ResultResponse.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultResponse.fail();
        }
    }
    
    @Override
    public CarbonVendor findByVendorId(String vendorId) {
        return vendorRepository.findByVendorId(vendorId);
    }

    @Override
    public List<CarbonVendor> findAll() {
        return vendorRepository.findAll();
    }
} 