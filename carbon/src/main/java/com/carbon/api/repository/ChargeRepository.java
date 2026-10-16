package com.carbon.api.repository;

import com.carbon.api.entity.CarbonCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeRepository extends JpaRepository<CarbonCharge, Long> {
    /**
     * 根據交易編號查詢充電交易
     */
    CarbonCharge findByTransNo(String transNo);
    
    /**
     * 根據廠商ID、場站ID和充電樁ID查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStationIdAndChargerId(String vendorId, String stationId, String chargerId);
    
    /**
     * 根據廠商ID和場站ID查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStationId(String vendorId, String stationId);
    
    /**
     * 根據廠商ID查詢充電交易
     */
    List<CarbonCharge> findByVendorId(String vendorId);
    
    /**
     * 根據場站ID查詢充電交易
     */
    List<CarbonCharge> findByStationId(String stationId);
    
    /**
     * 根據startTime範圍查詢充電交易
     */
    List<CarbonCharge> findByStartTimeBetween(String startTimeFrom, String startTimeTo);
    
    /**
     * 根據廠商ID和startTime範圍查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStartTimeBetween(String vendorId, String startTimeFrom, String startTimeTo);
    
    /**
     * 根據廠商ID、場站ID和startTime範圍查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStationIdAndStartTimeBetween(
            String vendorId, String stationId, String startTimeFrom, String startTimeTo);
    
    /**
     * 根據廠商ID、場站ID、充電樁ID和startTime範圍查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStationIdAndChargerIdAndStartTimeBetween(
            String vendorId, String stationId, String chargerId, String startTimeFrom, String startTimeTo);
    
    /**
     * 根據startTime大於等於指定時間查詢充電交易
     */
    List<CarbonCharge> findByStartTimeGreaterThanEqual(String startTimeFrom);
    
    /**
     * 根據startTime小於等於指定時間查詢充電交易
     */
    List<CarbonCharge> findByStartTimeLessThanEqual(String startTimeTo);
    
    /**
     * 根據廠商ID和startTime大於等於指定時間查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStartTimeGreaterThanEqual(String vendorId, String startTimeFrom);
    
    /**
     * 根據廠商ID和startTime小於等於指定時間查詢充電交易
     */
    List<CarbonCharge> findByVendorIdAndStartTimeLessThanEqual(String vendorId, String startTimeTo);
    
    /**
     * 根據場站ID和startTime範圍查詢充電交易（管理員使用）
     */
    List<CarbonCharge> findByStationIdAndStartTimeBetween(String stationId, String startTimeFrom, String startTimeTo);

    /**
     * 查詢指定充電樁槍頭在啟用日期起算的第一筆交易時間
     */
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT MIN(c.start_time) FROM carbon_charge c WHERE c.charger_id = :chargerId AND c.connect_id = :connectId AND c.start_time >= :minStartTime", 
        nativeQuery = true
    )
    String findFirstTransactionTime(
        @org.springframework.data.repository.query.Param("chargerId") String chargerId, 
        @org.springframework.data.repository.query.Param("connectId") String connectId, 
        @org.springframework.data.repository.query.Param("minStartTime") String minStartTime
    );
} 