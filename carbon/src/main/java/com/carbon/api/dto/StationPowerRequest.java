package com.carbon.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class StationPowerRequest {
    @NotBlank(message = "廠商ID(vendorId)不能為空")
    private String vendorId;
    
    @NotBlank(message = "場站ID(stationId)不能為空")
    private String stationId;
    
    private String stationKwh;
    
    @Valid
    private List<DailyPowerInfo> everyDayKwh;
    
    @Data
    public static class DailyPowerInfo {
        @NotBlank(message = "日期(dateInfo)不能為空")
        private String dateInfo;
        
        private String dateKwh;
    }
} 