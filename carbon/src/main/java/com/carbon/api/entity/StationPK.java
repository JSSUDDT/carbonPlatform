package com.carbon.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationPK implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vendorId;
    private String stationId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationPK stationPK = (StationPK) o;
        return Objects.equals(vendorId, stationPK.vendorId) &&
               Objects.equals(stationId, stationPK.stationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, stationId);
    }
}
