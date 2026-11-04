package com.carbon.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorPK implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String vendorId;
    private String stationId;
    private String chargerId;
    private String connectId;
    
    public ConnectorPK(String chargerId, String connectId) {
        this.chargerId = chargerId;
        this.connectId = connectId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectorPK that = (ConnectorPK) o;
        return Objects.equals(vendorId, that.vendorId) &&
               Objects.equals(stationId, that.stationId) &&
               Objects.equals(chargerId, that.chargerId) && 
               Objects.equals(connectId, that.connectId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(vendorId, stationId, chargerId, connectId);
    }
}
