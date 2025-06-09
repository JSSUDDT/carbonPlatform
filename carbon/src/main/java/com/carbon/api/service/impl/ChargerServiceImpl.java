package com.carbon.api.service.impl;

import com.carbon.api.dto.ChargerRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.ConnectorPK;
import com.carbon.api.repository.ChargerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.AuthorizationService;
import com.carbon.api.service.ChargerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChargerServiceImpl implements ChargerService {

    private static final Logger logger = LoggerFactory.getLogger(ChargerServiceImpl.class);
    
    @Autowired
    private ChargerRepository chargerRepository;

    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private AuthorizationService authorizationService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public ResultResponse updateChargerInfo(ChargerRequest request) {
        try {
            // 檢查請求和充電樁列表是否為null
            if (request == null) {
                logger.error("Request object is null");
                return new ResultResponse("0001", "Invalid request data");
            }
            
            // 檢查權限 - 確保調用者有權訪問該廠商的資源
            if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
                logger.error("User has no access to this vendor resource: {}", request.getVendorId());
                return new ResultResponse("0003", "No permission to access this vendor resource");
            }
            
            logger.debug("Received charger information update request, vendorId: {}, stationId: {}", request.getVendorId(), request.getStationId());
            
            // 檢查場站是否存在
            CarbonStation station = stationRepository.findByStationId(request.getStationId());
            if (station == null) {
                logger.error("Station does not exist, stationId: {}", request.getStationId());
                return new ResultResponse("0004", "Station does not exist: " + request.getStationId());
            }
            
            // 檢查場站是否屬於該廠商或用戶是否為管理員
            if (!station.getVendorId().equals(request.getVendorId()) && !authorizationService.isCurrentUserAdmin()) {
                logger.error("Station does not belong to requesting vendor, requestVendorId: {}, stationVendorId: {}, stationId: {}", 
                        request.getVendorId(), request.getStationId(), station.getVendorId());
                return new ResultResponse("0003", "No permission to operate on another vendor's station: " + request.getStationId());
            }
            
            // 獲取充電樁列表，如果為null則初始化為空列表
            List<ChargerRequest.ChargerInfo> chargerInfoList = request.getChargerAry();
            if (chargerInfoList == null) {
                logger.warn("Charger list is null, vendorId: {}, stationId: {}", request.getVendorId(), request.getStationId());
                chargerInfoList = new ArrayList<>(); // 初始化為空列表避免NPE
            }
            
            logger.debug("Processing {} charger records", chargerInfoList.size());
            
            for (ChargerRequest.ChargerInfo chargerInfo : chargerInfoList) {
                if (chargerInfo == null) {
                    logger.warn("Skipping null charger info");
                    continue;
                }
                
                String updateStatus = chargerInfo.getUpdateStatus();
                
                // 根據updateStatus執行不同操作
                if ("D".equals(updateStatus)) {
                    // 刪除操作前先檢查充電樁是否存在及是否屬於該廠商
                    List<CarbonCharger> existingChargers = chargerRepository.findByVendorIdAndStationIdAndChargerId(
                            request.getVendorId(), request.getStationId(), chargerInfo.getChargerId());
                    
                    if (existingChargers == null || existingChargers.isEmpty()) {
                        logger.warn("Charger to be deleted does not exist, vendorId: {}, stationId: {}, chargerId: {}", 
                                request.getVendorId(), request.getStationId(), chargerInfo.getChargerId());
                        continue;
                    }
                    
                    // 刪除操作 - 刪除所有該充電樁相關記錄
                    logger.debug("Deleting charger, chargerId: {}", chargerInfo.getChargerId());
                    chargerRepository.deleteByVendorIdAndStationIdAndChargerId(
                            request.getVendorId(), request.getStationId(), chargerInfo.getChargerId());
                } else if ("U".equals(updateStatus)) {
                    // U: 新增或更新
                    logger.debug("Adding or updating charger, chargerId: {}, connectId: {}", 
                            chargerInfo.getChargerId(), chargerInfo.getConnectId());
                    
                    // 檢查充電樁和充電槍頭是否存在
                    CarbonCharger charger = chargerRepository.findByChargerIdAndConnectId(
                        chargerInfo.getChargerId(), chargerInfo.getConnectId());
                    
                    // 若存在，檢查是否屬於該廠商或用戶是否為管理員
                    if (charger != null && !charger.getVendorId().equals(request.getVendorId()) && !authorizationService.isCurrentUserAdmin()) {
                        logger.error("Attempting to update another vendor's charger, requestVendorId: {}, chargerVendorId: {}, chargerId: {}", 
                                request.getVendorId(), charger.getVendorId(), chargerInfo.getChargerId());
                        return new ResultResponse("0003", "No permission to operate on another vendor's charger: " + chargerInfo.getChargerId());
                    }
                    
                    if (charger == null) {
                        // 新增充電樁和充電槍頭
                        logger.debug("Adding new charger and connector, chargerId: {}, connectId: {}", 
                                chargerInfo.getChargerId(), chargerInfo.getConnectId());
                        charger = new CarbonCharger(
                                chargerInfo.getChargerId(), 
                                chargerInfo.getConnectId(),
                                request.getVendorId(), 
                                request.getStationId()
                        );
                        charger.setCreateTime(LocalDateTime.now().format(formatter));
                    } else {
                        logger.debug("Updating existing charger and connector, chargerId: {}, connectId: {}", 
                                chargerInfo.getChargerId(), chargerInfo.getConnectId());
                    }
                    
                    // 設置充電樁屬性
                    charger.setChargerType(chargerInfo.getChargerType());
                    charger.setChargerBrand(chargerInfo.getChargerBrand());
                    charger.setChargerStatus(chargerInfo.getChargerStatus());
                    charger.setChargerKw(chargerInfo.getChargerKw());
                    charger.setChargerA(chargerInfo.getChargerA());
                    charger.setChargerV(chargerInfo.getChargerV());
                    charger.setUpdateTime(LocalDateTime.now().format(formatter));
                    
                    // 保存充電樁和充電槍頭
                    chargerRepository.save(charger);
                } else {
                    logger.error("Unsupported update status: {}, chargerId: {}", updateStatus, chargerInfo.getChargerId());
                    return new ResultResponse("0002", "Unsupported update status: " + updateStatus);
                }
            }
            
            logger.debug("Charger information update completed");
            return ResultResponse.success();
        } catch (Exception e) {
            logger.error("Error occurred when updating charger information", e);
            return ResultResponse.fail();
        }
    }

    @Override
    public List<CarbonCharger> getChargerInfo(String vendorId, String stationId) {
        logger.debug("Getting charger information, vendorId: {}, stationId: {}", vendorId, stationId);
        
        List<CarbonCharger> chargers;
        // 根據條件查詢充電樁信息
        if (vendorId != null && stationId != null) {
            // 查詢指定廠商和場站的充電樁
            chargers = chargerRepository.findByVendorIdAndStationId(vendorId, stationId);
        } else if (vendorId != null) {
            // 查詢指定廠商的所有充電樁
            chargers = chargerRepository.findByVendorId(vendorId);
        } else if (stationId != null) {
            // 查詢指定場站的所有充電樁
            chargers = chargerRepository.findByStationId(stationId);
        } else {
            // 如果未指定條件，則返回全部充電樁
            chargers = chargerRepository.findAll();
        }

        // 為每個充電樁添加充電站名稱
        for (CarbonCharger charger : chargers) {
            CarbonStation station = stationRepository.findByStationId(charger.getStationId());
            if (station != null) {
                charger.setStationName(station.getStationName());
            }
        }

        return chargers;
    }

    @Override
    public List<CarbonCharger> getChargerInfoById(String chargerId) {
        logger.debug("Getting charger information by ID, chargerId: {}", chargerId);
        List<CarbonCharger> chargers = chargerRepository.findByChargerId(chargerId);
        
        // 為每個充電樁添加充電站名稱
        for (CarbonCharger charger : chargers) {
            CarbonStation station = stationRepository.findByStationId(charger.getStationId());
            if (station != null) {
                charger.setStationName(station.getStationName());
            }
        }
        
        return chargers;
    }
    
    @Override
    public CarbonCharger getChargerInfoByIdAndConnectId(String chargerId, String connectId) {
        logger.debug("Getting charger information by ID and connectId, chargerId: {}, connectId: {}", chargerId, connectId);
        CarbonCharger charger = chargerRepository.findByChargerIdAndConnectId(chargerId, connectId);
        
        if (charger != null) {
            // 為充電樁添加充電站名稱
            CarbonStation station = stationRepository.findByStationId(charger.getStationId());
            if (station != null) {
                charger.setStationName(station.getStationName());
            }
        }
        
        return charger;
    }

    @Override
    public List<CarbonCharger> getChargerInfo(String vendorId, String stationId, String search, String status) {
        logger.debug("Getting charger information with filters, vendorId: {}, stationId: {}, search: {}, status: {}", 
            vendorId, stationId, search, status);
        
        // 首先獲取基礎充電樁列表
        List<CarbonCharger> chargers;
        if (vendorId != null && stationId != null) {
            // 查詢指定廠商和場站的充電樁
            chargers = chargerRepository.findByVendorIdAndStationId(vendorId, stationId);
        } else if (vendorId != null) {
            // 查詢指定廠商的所有充電樁
            chargers = chargerRepository.findByVendorId(vendorId);
        } else if (stationId != null) {
            // 查詢指定場站的所有充電樁
            chargers = chargerRepository.findByStationId(stationId);
        } else {
            // 如果未指定條件，則返回全部充電樁
            chargers = chargerRepository.findAll();
        }
        
        // 為每個充電樁添加充電站名稱
        for (CarbonCharger charger : chargers) {
            CarbonStation station = stationRepository.findByStationId(charger.getStationId());
            if (station != null) {
                charger.setStationName(station.getStationName());
            }
        }
        
        // 如果沒有搜索條件和狀態篩選，直接返回
        if ((search == null || search.isEmpty()) && (status == null || status.isEmpty())) {
            return chargers;
        }
        
        // 應用篩選條件
        List<CarbonCharger> filteredChargers = new ArrayList<>();
        
        for (CarbonCharger charger : chargers) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            
            // 搜索條件篩選
            if (search != null && !search.isEmpty()) {
                // 在充電樁ID、類型、品牌中搜索
                matchesSearch = charger.getChargerId().toLowerCase().contains(search.toLowerCase())
                        || (charger.getChargerType() != null && charger.getChargerType().toLowerCase().contains(search.toLowerCase()))
                        || (charger.getChargerBrand() != null && charger.getChargerBrand().toLowerCase().contains(search.toLowerCase()))
                        || (charger.getConnectId() != null && charger.getConnectId().toLowerCase().contains(search.toLowerCase()));
            }
            
            // 狀態篩選
            if (status != null && !status.isEmpty()) {
                matchesStatus = status.equals(charger.getChargerStatus());
            }
            
            // 同時滿足搜索和狀態條件
            if (matchesSearch && matchesStatus) {
                filteredChargers.add(charger);
            }
        }
        
        return filteredChargers;
    }
} 