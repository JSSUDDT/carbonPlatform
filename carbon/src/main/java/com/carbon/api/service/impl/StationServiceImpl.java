package com.carbon.api.service.impl;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.StationRequest;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.AuthorizationService;
import com.carbon.api.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationServiceImpl.class);

    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private AuthorizationService authorizationService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public ResultResponse updateStationInfo(StationRequest request) {
        try {
            // 檢查請求和場站列表是否為null
            if (request == null) {
                logger.error("Request object is null");
                return new ResultResponse("0001", "Invalid request data");
            }
            
            // 再次檢查權限 - 確保調用者有權訪問該廠商的資源
            if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
                logger.error("User has no access to this vendor resource: {}", request.getVendorId());
                return new ResultResponse("0003", "No permission to access this vendor resource");
            }
            
            logger.debug("Received station information update request, vendorId: {}", request.getVendorId());
            
            // 獲取場站列表，如果為null則初始化為空列表
            List<StationRequest.StationInfo> stationInfoList = request.getStationAry();
            if (stationInfoList == null) {
                logger.warn("Station list is null, vendorId: {}", request.getVendorId());
                stationInfoList = new ArrayList<>(); // 初始化為空列表避免NPE
            }
            
            logger.debug("Processing {} station information", stationInfoList.size());
            
            for (StationRequest.StationInfo stationInfo : stationInfoList) {
                if (stationInfo == null) {
                    logger.warn("Skipping null station information");
                    continue;
                }
                
                String updateStatus = stationInfo.getUpdateStatus();
                
                // 根據updateStatus執行不同操作
                if ("D".equals(updateStatus)) {
                    // 刪除操作
                    logger.debug("Deleting station, stationId: {}", stationInfo.getStationId());
                    CarbonStation station = stationRepository.findByVendorIdAndStationId(
                            request.getVendorId(), stationInfo.getStationId());
                    if (station != null) {
                        // 確認該場站屬於請求的廠商
                        if (!station.getVendorId().equals(request.getVendorId())) {
                            logger.error("Attempt to delete another vendor's station, requestVendorId: {}, stationId: {}", 
                                    request.getVendorId(), stationInfo.getStationId());
                            return new ResultResponse("0003", "No permission to operate on another vendor's station: " + stationInfo.getStationId());
                        }
                        stationRepository.delete(station);
                    } else {
                        logger.warn("Station to be deleted does not exist, vendorId: {}, stationId: {}", 
                                request.getVendorId(), stationInfo.getStationId());
                    }
                } else if ("U".equals(updateStatus)) {
                    // 新增或更新操作
                    CarbonStation station = stationRepository.findByStationId(stationInfo.getStationId());
                    
                    if (station == null) {
                        // 新增
                        logger.debug("Adding new station, stationId: {}", stationInfo.getStationId());
                        station = new CarbonStation();
                        station.setCreateTime(LocalDateTime.now().format(formatter));
                    } else {
                        // 更新 - 確認該場站屬於請求的廠商
                        if (!station.getVendorId().equals(request.getVendorId())) {
                            logger.error("Attempt to update another vendor's station, requestVendorId: {}, stationVendorId: {}, stationId: {}", 
                                    request.getVendorId(), station.getVendorId(), stationInfo.getStationId());
                            return new ResultResponse("0003", "No permission to operate on another vendor's station: " + stationInfo.getStationId());
                        }
                        logger.debug("Updating station, stationId: {}", stationInfo.getStationId());
                    }
                    
                    // 設置屬性
                    station.setStationId(stationInfo.getStationId());
                    station.setVendorId(request.getVendorId());
                    station.setStationName(stationInfo.getStationName());
                    station.setStationKwh(stationInfo.getStationKwh());
                    station.setCity(stationInfo.getCity());
                    station.setAddress(stationInfo.getAddress());
                    station.setStorageEnable(stationInfo.getStorageEnable());
                    station.setPowerSource(stationInfo.getPowerSource());
                    station.setGpsLatitude(stationInfo.getGpsLatitude());
                    station.setGpsLongitude(stationInfo.getGpsLongitude());
                    station.setStationStatus(stationInfo.getStationStatus());
                    station.setUpdateTime(LocalDateTime.now().format(formatter));
                    
                    // 保存
                    stationRepository.save(station);
                } else {
                    // 不支持的操作類型
                    logger.error("Unsupported update status: {}, stationId: {}", updateStatus, stationInfo.getStationId());
                    return new ResultResponse("0002", "Unsupported update status: " + updateStatus);
                }
            }
            
            logger.debug("Station information update completed");
            return ResultResponse.success();
        } catch (Exception e) {
            logger.error("Error occurred when updating station information: {}", e.getMessage(), e);
            return ResultResponse.fail();
        }
    }

    @Override
    public List<CarbonStation> getStationInfo(String vendorId) {
        logger.debug("Getting station information, vendorId: {}", vendorId);
        
        // 根據vendorId查詢充電站信息
        if (vendorId != null) {
            return stationRepository.findByVendorId(vendorId);
        } else {
            // 如果未指定vendorId，則返回全部充電站
            return stationRepository.findAll();
        }
    }

    @Override
    public List<CarbonStation> getStationInfo(String vendorId, String search, String status) {
        logger.debug("Getting station information, vendorId: {}, search: {}, status: {}", vendorId, search, status);
        
        // 獲取基礎充電站列表
        List<CarbonStation> stations;
        if (vendorId != null) {
            stations = stationRepository.findByVendorId(vendorId);
        } else {
            stations = stationRepository.findAll();
        }
        
        // 如果沒有搜索條件和狀態篩選，直接返回
        if ((search == null || search.isEmpty()) && (status == null || status.isEmpty())) {
            return stations;
        }
        
        // 應用篩選條件
        List<CarbonStation> filteredStations = new ArrayList<>();
        
        for (CarbonStation station : stations) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            
            // 搜索條件篩選
            if (search != null && !search.isEmpty()) {
                // 在站點ID、站點名稱、城市或地址中搜索
                matchesSearch = station.getStationId().toLowerCase().contains(search.toLowerCase())
                        || (station.getStationName() != null && station.getStationName().toLowerCase().contains(search.toLowerCase()))
                        || (station.getCity() != null && station.getCity().toLowerCase().contains(search.toLowerCase()))
                        || (station.getAddress() != null && station.getAddress().toLowerCase().contains(search.toLowerCase()));
            }
            
            // 狀態篩選
            if (status != null && !status.isEmpty()) {
                matchesStatus = status.equals(station.getStationStatus());
            }
            
            // 同時滿足搜索和狀態條件
            if (matchesSearch && matchesStatus) {
                filteredStations.add(station);
            }
        }
        
        return filteredStations;
    }

    @Override
    public CarbonStation getStationInfoById(String stationId) {
        logger.debug("根據ID獲取充電站信息，stationId: {}", stationId);
        return stationRepository.findByStationId(stationId);
    }
} 