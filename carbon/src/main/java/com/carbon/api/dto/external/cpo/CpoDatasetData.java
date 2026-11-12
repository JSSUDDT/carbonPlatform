package com.carbon.api.dto.external.cpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpoDatasetData {
    @Builder.Default
    private List<CpoStationDto> stations = new ArrayList<>();
    
    @Builder.Default
    private List<CpoChargerDto> chargers = new ArrayList<>();
    
    @Builder.Default
    private List<CpoTransactionDto> transactions = new ArrayList<>();
}
