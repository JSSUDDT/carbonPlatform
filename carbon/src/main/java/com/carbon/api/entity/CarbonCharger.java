package com.carbon.api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@IdClass(ConnectorPK.class)
@Table(name = "carbon_charger")
public class CarbonCharger implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "charger_id", length = 50)
    private String chargerId;

    @Id
    @Column(name = "connect_id", length = 50)
    private String connectId;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    @Column(name = "vendor_id", nullable = false, length = 50)
    private String vendorId;

    @Column(name = "charger_type", length = 10)
    private String chargerType;

    @Column(name = "charger_brand", length = 50)
    private String chargerBrand;

    @Column(name = "charger_kw", length = 50)
    private String chargerKw;

    @Column(name = "charger_a", length = 50)
    private String chargerA;

    @Column(name = "charger_v", length = 50)
    private String chargerV;

    @Column(name = "charger_status", length = 1)
    private String chargerStatus;

    @Column(name = "create_time")
    private String createTime;
    
    @Column(name = "update_time")
    private String updateTime;
    
    @Transient
    private String stationName;
    
    public CarbonCharger(String chargerId, String connectId, String vendorId, String stationId) {
        this.chargerId = chargerId;
        this.connectId = connectId;
        this.vendorId = vendorId;
        this.stationId = stationId;
    }
} 