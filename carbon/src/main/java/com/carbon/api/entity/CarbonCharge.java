package com.carbon.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "carbon_charge")
public class CarbonCharge implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "trans_no", length = 50)
    private String transNo;

    @Column(name = "vendor_id", nullable = false, length = 50)
    private String vendorId;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    @Column(name = "charger_id", nullable = false, length = 50)
    private String chargerId;

    @Column(name = "connect_id", length = 50)
    private String connectId;

    @Column(name = "invoice_no", length = 50)
    private String invoiceNo;
    
    @Column(name = "customer_id", length = 50)
    private String customerId;
    
    @Column(name = "invoice_date", length = 20)
    private String invoiceDate;
    
    @Column(name = "carbon_credit", length = 1)
    private String carbonCredit;

    @Column(name = "start_time", length = 20)
    private String startTime;

    @Column(name = "end_time", length = 20)
    private String endTime;

    @Column(name = "kwh", length = 50)
    private String kwh;

    @Column(name = "car_type", length = 10)
    private String carType;

    @Column(name = "car_tonnage", length = 50)
    private String carTonnage;

    @Column(name = "car_no", length = 20)
    private String carNo;

    @Column(name = "create_time")
    private String createTime;
    
    @Column(name = "update_time")
    private String updateTime;
} 