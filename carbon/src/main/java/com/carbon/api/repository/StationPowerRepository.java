package com.carbon.api.repository;

import com.carbon.api.entity.CarbonStationPower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationPowerRepository extends JpaRepository<CarbonStationPower, Long> {
    /**
     * 根據廠商ID和場站ID及日期查詢場站負載
     */
    CarbonStationPower findByVendorIdAndStationIdAndDateInfo(String vendorId, String stationId, String dateInfo);
    
    /**
     * 根據廠商ID和場站ID查詢場站負載
     */
    List<CarbonStationPower> findByVendorIdAndStationId(String vendorId, String stationId);
    
    /**
     * 根據廠商ID查詢場站負載
     */
    List<CarbonStationPower> findByVendorId(String vendorId);
    
    /**
     * 根據場站ID查詢場站負載
     */
    List<CarbonStationPower> findByStationId(String stationId);
} 