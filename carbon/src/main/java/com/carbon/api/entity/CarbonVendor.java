package com.carbon.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "carbon_vendor")
public class CarbonVendor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "vendor_id", length = 50)
    private String vendorId;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "status", length = 1)
    private String status;
    
    @Column(name = "role", length = 20)
    private String role = "ROLE_USER"; // 默认为普通用户角色
    
    @Column(name = "create_time")
    private String createTime;
    
    @Column(name = "update_time")
    private String updateTime;
} 