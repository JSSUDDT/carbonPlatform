package com.carbon.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "carbon_station_power")
public class CarbonStationPower implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "vendor_id", nullable = false, length = 50)
    private String vendorId;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    @Column(name = "station_kwh", length = 50)
    private String stationKwh;

    @Column(name = "date_info", length = 10)
    private String dateInfo;

    @Column(name = "date_kwh", length = 50)
    private String dateKwh;

    @Column(name = "create_time")
    private String createTime;
    
    @Column(name = "update_time")
    private String updateTime;
} 