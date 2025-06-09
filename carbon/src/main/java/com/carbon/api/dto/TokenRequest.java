package com.carbon.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class TokenRequest {
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    private String vendorId;
    
    @NotBlank(message = "密碼(password)不能為空")
    private String password;
} 