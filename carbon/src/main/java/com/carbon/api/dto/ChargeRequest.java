package com.carbon.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ChargeRequest {
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    private String vendorId;
    
    @Valid
    private List<TransInfo> transAry;
    
    @Data
    public static class TransInfo {
        @NotBlank(message = "場站ID(stationId)不能為空")
        private String stationId;
        
        @NotBlank(message = "充電樁ID(chargerId)不能為空")
        private String chargerId;
        
        private String connectId;
        
        @NotBlank(message = "交易編號(transNo)不能為空")
        private String transNo;
        
        private String invoiceNo;
        private String customerId;
        private String invoiceDate;
        private String carbonCredit;
        private String startTime;
        private String endTime;
        private String kwh;
        private String carType;
        private String carTonnage;
        private String carNo;
    }
} 