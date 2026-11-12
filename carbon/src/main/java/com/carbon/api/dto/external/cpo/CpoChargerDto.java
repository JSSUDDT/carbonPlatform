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
public class CpoChargerDto {
    private String stationId;
    private String chargerId;
    private String connectId;
    private String firstEnableDate;
    private String chargerType;
    private String chargerKw;
}
