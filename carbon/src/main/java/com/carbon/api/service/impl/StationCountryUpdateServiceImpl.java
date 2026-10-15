package com.carbon.api.service.impl;

import com.carbon.api.entity.CarbonStation;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.GeocodingService;
import com.carbon.api.service.StationCountryUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 充電站國家資訊批次更新服務實現
 */
@Service
public class StationCountryUpdateServiceImpl implements StationCountryUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(StationCountryUpdateServiceImpl.class);
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private GeocodingService geocodingService;
    
    @Override
    @Transactional
    public UpdateResult updateAllStationsCountry(boolean forceUpdate, int limit) {
        logger.info("Starting batch update of country information for all stations, force update mode: {}, limit: {}", 
                forceUpdate, limit > 0 ? limit : "unlimited");
        
        // 獲取所有充電站
        List<CarbonStation> allStations = stationRepository.findAll();
        int totalStations = allStations.size();
        logger.info("Found {} stations", totalStations);
        
        // 如果設定了 limit，只處理前 N 個需要更新的站點
        if (limit > 0 && limit < totalStations) {
            logger.info("Limit specified: will process up to {} stations", limit);
        }
        
        int processedStations = 0;
        int updatedStations = 0;
        int skippedStations = 0;
        int failedStations = 0;
        int stationsNeedingUpdate = 0;
        
        // 先計算需要更新的站點數量（用於 limit 判斷）
        if (limit > 0) {
            for (CarbonStation station : allStations) {
                boolean needsUpdate = forceUpdate || 
                    station.getCountry() == null || 
                    station.getCountry().trim().isEmpty() || 
                    "NA".equals(station.getCountry());
                if (needsUpdate) {
                    stationsNeedingUpdate++;
                }
            }
            logger.info("Stations needing update: {} (limit: {})", stationsNeedingUpdate, limit);
        }
        
        for (CarbonStation station : allStations) {
            // 如果設定了 limit 且已達到更新數量限制，停止處理
            if (limit > 0 && updatedStations >= limit) {
                logger.info("Reached update limit ({}), stopping processing", limit);
                break;
            }
            
            processedStations++;
            
            try {
                // 檢查是否需要更新
                boolean needsUpdate = forceUpdate || 
                    station.getCountry() == null || 
                    station.getCountry().trim().isEmpty() || 
                    "NA".equals(station.getCountry());
                
                if (!needsUpdate) {
                    skippedStations++;
                    logger.debug("Skipping station {}, already has country code: {}", station.getStationId(), station.getCountry());
                    continue;
                }
                
                // 檢查是否有經緯度
                if (station.getGpsLatitude() == null || station.getGpsLatitude().trim().isEmpty() ||
                    station.getGpsLongitude() == null || station.getGpsLongitude().trim().isEmpty()) {
                    // 沒有經緯度，設置為 NA
                    station.setCountry("NA");
                    station.setUpdateTime(LocalDateTime.now().format(formatter));
                    stationRepository.save(station);
                    updatedStations++;
                    logger.warn("Station {} has no GPS coordinates, setting country code to NA", station.getStationId());
                    continue;
                }
                
                // 調用 OpenCage API 獲取國家代碼
                logger.info("Processing station {}/{}: {} (lat={}, lng={})", 
                        processedStations, totalStations, 
                        station.getStationId(), 
                        station.getGpsLatitude(), 
                        station.getGpsLongitude());
                
                try {
                    String countryCode = geocodingService.getCountryCode(
                            station.getGpsLatitude(), 
                            station.getGpsLongitude());
                    
                    if ("NA".equals(countryCode)) {
                        logger.warn("Station {} received NA from geocoding service (lat={}, lng={})", 
                                station.getStationId(), station.getGpsLatitude(), station.getGpsLongitude());
                    }
                    
                    station.setCountry(countryCode);
                    station.setUpdateTime(LocalDateTime.now().format(formatter));
                    stationRepository.save(station);
                    
                    updatedStations++;
                    logger.info("Successfully updated station {} country code: {}", station.getStationId(), countryCode);
                    
                } catch (Exception e) {
                    // API 調用失敗，設置為 NA
                    logger.error("Error getting country code for station {}, setting to NA: {}", 
                            station.getStationId(), e.getMessage(), e);
                    station.setCountry("NA");
                    station.setUpdateTime(LocalDateTime.now().format(formatter));
                    stationRepository.save(station);
                    updatedStations++; // 雖然失敗但還是更新了（設為 NA）
                }
                
            } catch (Exception e) {
                failedStations++;
                logger.error("Error processing station {}: {}", station.getStationId(), e.getMessage(), e);
            }
            
            // 每處理 10 個站點記錄一次進度
            if (processedStations % 10 == 0) {
                logger.info("Progress: {}/{} processed, {} updated, {} skipped, {} failed", 
                        processedStations, totalStations, updatedStations, skippedStations, failedStations);
            }
        }
        
        String message = String.format(
                "Batch update completed: Total %d stations, %d processed, %d updated, %d skipped, %d failed",
                totalStations, processedStations, updatedStations, skippedStations, failedStations);
        
        logger.info(message);
        
        return new UpdateResult(totalStations, processedStations, updatedStations, 
                               skippedStations, failedStations, message);
    }
}

