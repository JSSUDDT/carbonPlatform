package com.carbon.api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
public class ConnectorPK implements Serializable {
    
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
        return Objects.equals(chargerId, that.chargerId) && 
               Objects.equals(connectId, that.connectId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(chargerId, connectId);
    }
} 