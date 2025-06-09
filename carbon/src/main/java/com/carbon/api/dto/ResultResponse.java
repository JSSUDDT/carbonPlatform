package com.carbon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultResponse {
    private String resultCode;
    private String resultMsg;
    
    public static ResultResponse success() {
        return new ResultResponse("0000", "Success");
    }
    
    public static ResultResponse fail() {
        return new ResultResponse("0001", "Fail");
    }
} 