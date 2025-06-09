package com.carbon.api.repository;

import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.ConnectorPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerRepository extends JpaRepository<CarbonCharger, Long> {
    List<CarbonCharger> findByVendorId(String vendorId);
    List<CarbonCharger> findByStationId(String stationId);
    List<CarbonCharger> findByVendorIdAndStationId(String vendorId, String stationId);
    CarbonCharger findByChargerIdAndConnectId(String chargerId, String connectId);
    List<CarbonCharger> findByChargerId(String chargerId);
    List<CarbonCharger> findByVendorIdAndStationIdAndChargerId(String vendorId, String stationId, String chargerId);
    CarbonCharger findByVendorIdAndStationIdAndChargerIdAndConnectId(String vendorId, String stationId, String chargerId, String connectId);
    void deleteByVendorIdAndStationIdAndChargerId(String vendorId, String stationId, String chargerId);

    /**
     * 根據充電樁ID和場站ID查詢充電樁
     */
    CarbonCharger findByChargerIdAndStationId(String chargerId, String stationId);
    
    /**
     * 查詢指定場站中的充電槍頭数量
     */
    @Query("SELECT COUNT(c) FROM CarbonCharger c WHERE c.stationId = ?1")
    long countByStationId(String stationId);
} 