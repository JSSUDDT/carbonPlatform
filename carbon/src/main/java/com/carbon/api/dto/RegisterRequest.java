package com.carbon.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    @Size(min = 4, max = 50, message = "廠商ID長度應在4-50之間")
    private String vendorId;
    
    @NotBlank(message = "密碼(password)不能為空")
    @Size(min = 6, max = 50, message = "密碼長度應在6-50之間")
    private String password;
    
    private String status = "1"; // 默認啟用狀態
    
    private String role = "ROLE_USER"; // 默認為普通用戶角色，可以指定為"ROLE_ADMIN"或"ROLE_USER"
} 