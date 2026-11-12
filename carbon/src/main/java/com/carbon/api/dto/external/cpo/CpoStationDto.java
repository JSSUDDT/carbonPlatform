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
public class CpoStationDto {
    private String stationId;
    private String stationName;
    private String gpsLatitude;
    private String gpsLongitude;
    private String address;
}
