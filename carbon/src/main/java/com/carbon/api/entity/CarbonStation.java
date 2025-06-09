package com.carbon.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "carbon_station")
public class CarbonStation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "station_id", length = 50)
    private String stationId;

    @Column(name = "vendor_id", nullable = false, length = 50)
    private String vendorId;

    @Column(name = "station_name", length = 100)
    private String stationName;

    @Column(name = "station_kwh", length = 50)
    private String stationKwh;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "storage_enable", length = 1)
    private String storageEnable;

    @Column(name = "power_source", length = 1)
    private String powerSource;

    @Column(name = "gps_latitude", length = 50)
    private String gpsLatitude;

    @Column(name = "gps_longitude", length = 50)
    private String gpsLongitude;

    @Column(name = "station_status", length = 1)
    private String stationStatus;

    @Column(name = "create_time")
    private String createTime;
    
    @Column(name = "update_time")
    private String updateTime;
} 