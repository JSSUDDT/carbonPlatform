package com.carbon.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carbon_cpo_config")
public class CarbonCpoConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false, unique = true, length = 50)
    private String vendorId;

    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    @Column(name = "api_url", nullable = false, length = 255)
    private String apiUrl;

    @Column(name = "token", length = 500)
    private String token;

    @Column(name = "enabled", length = 1)
    @Builder.Default
    private String enabled = "1"; // 1: 啟用, 0: 停用

    @Column(name = "sync_interval_mins")
    @Builder.Default
    private Integer syncIntervalMins = 15;

    @Column(name = "last_sync_time", length = 30)
    private String lastSyncTime;

    @Column(name = "last_sync_status", length = 20)
    private String lastSyncStatus; // SUCCESS / FAILED

    @Column(name = "last_sync_message", length = 500)
    private String lastSyncMessage;

    @Column(name = "create_time", length = 30)
    private String createTime;

    @Column(name = "update_time", length = 30)
    private String updateTime;
}
