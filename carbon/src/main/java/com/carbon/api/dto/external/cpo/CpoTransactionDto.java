package com.carbon.api.dto.external.cpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpoTransactionDto {
    private String transNo;
    private String stationId;
    private String chargerId;
    private String connectId;
    private String startTime;
    private String endTime;
    private Object kwh;
    private String carType;
    private String carNo;
    private String invoiceNo;
}
