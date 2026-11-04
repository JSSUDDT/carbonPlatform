package com.carbon.api.repository;

import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.StationPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<CarbonStation, StationPK> {
    /**
     * 根據場站ID和廠商ID查詢場站
     */
    CarbonStation findByStationIdAndVendorId(String stationId, String vendorId);
    
    /**
     * 根據廠商ID和場站ID查詢場站
     */
    CarbonStation findByVendorIdAndStationId(String vendorId, String stationId);
    
    /**
     * 根據場站ID查詢場站列表（因不同業者可能取相同 stationId）
     */
    List<CarbonStation> findByStationId(String stationId);
    
    /**
     * 根據廠商ID查詢場站
     */
    List<CarbonStation> findByVendorId(String vendorId);
    
    /**
     * 刪除指定廠商和場站ID的記錄
     */
    void deleteByVendorIdAndStationId(String vendorId, String stationId);
}
