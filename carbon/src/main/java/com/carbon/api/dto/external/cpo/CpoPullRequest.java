package com.carbon.api.dto.external.cpo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpoPullRequest {
    private String vendorId;
    private String apiUrl;
    private String token;
    private String startTime;
    private String endTime;
    private Integer pageSize;
}
