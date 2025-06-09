package com.carbon.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ChargerRequest {
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    private String vendorId;
    
    @NotBlank(message = "場站ID(stationId)不能為空")
    private String stationId;
    
    @Valid
    private List<ChargerInfo> chargerAry;
    
    @Data
    public static class ChargerInfo {
        @NotBlank(message = "充電樁ID(chargerId)不能為空")
        private String chargerId;
        
        private String chargerType;
        private String chargerBrand;
        private String chargerStatus;
        @NotBlank(message = "更新狀態(updateStatus)不能為空，U=新增或更新，D=刪除")
        private String updateStatus;
        private String chargerKw;
        private String chargerA;
        private String chargerV;
        
        @NotBlank(message = "連接頭ID(connectId)不能為空")
        private String connectId;
    }
} 