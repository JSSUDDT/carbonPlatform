package com.carbon.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class StationRequest {
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    private String vendorId;
    
    @Valid
    private List<StationInfo> stationAry;
    
    @Data
    public static class StationInfo {
        @NotBlank(message = "場站ID(stationId)不能為空")
        private String stationId;
        
        private String stationName;
        private String stationKwh;
        private String city;
        private String address;
        private String storageEnable;
        private String powerSource;
        private String gpsLatitude;
        private String gpsLongitude;
        private String stationStatus;
        @NotBlank(message = "更新狀態(updateStatus)不能為空，U=新增或更新，D=刪除")
        private String updateStatus;
    }
}