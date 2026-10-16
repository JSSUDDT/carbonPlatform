package com.carbon.api.service.impl;

import com.carbon.api.dto.ChargeRequest;
import com.carbon.api.dto.ResultResponse;
import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.repository.ChargerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.AuthorizationService;
import com.carbon.api.service.ChargeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChargeServiceImpl implements ChargeService {

    private static final Logger logger = LoggerFactory.getLogger(ChargeServiceImpl.class);

    @Autowired
    private ChargeRepository chargeRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private ChargerRepository chargerRepository;
    
    @Autowired
    private AuthorizationService authorizationService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public ResultResponse updateChargeInfo(ChargeRequest request) {
        try {
            // Check if request is null
            if (request == null) {
                logger.error("Request object is null");
                return new ResultResponse("0001", "Invalid request data");
            }
            
            // Check permission - ensure caller has access to vendor resources
            if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
                logger.error("User has no access to this vendor resource: {}", request.getVendorId());
                return new ResultResponse("0003", "No permission to access this vendor resource");
            }
            
            logger.debug("Received charge information update request, vendorId: {}", request.getVendorId());
            
            // Get transaction list, initialize as empty if null
            List<ChargeRequest.TransInfo> transInfoList = request.getTransAry();
            if (transInfoList == null) {
                logger.warn("Transaction list is null, vendorId: {}", request.getVendorId());
                transInfoList = new ArrayList<>(); // Initialize as empty list to avoid NPE
            }
            
            logger.debug("Processing {} transaction records", transInfoList.size());
            
            for (ChargeRequest.TransInfo transInfo : transInfoList) {
                if (transInfo == null) {
                    logger.warn("Skipping null transaction info");
                    continue;
                }
                
                // Check if station exists
                CarbonStation station = stationRepository.findByStationId(transInfo.getStationId());
                if (station == null) {
                    logger.error("Station does not exist, stationId: {}", transInfo.getStationId());
                    return new ResultResponse("0004", "Station does not exist: " + transInfo.getStationId());
                }
                
                // Check if station belongs to requesting vendor or user is admin
                if (!station.getVendorId().equals(request.getVendorId()) && !authorizationService.isCurrentUserAdmin()) {
                    logger.error("Station does not belong to requesting vendor, requestVendorId: {}, stationVendorId: {}, stationId: {}", 
                            request.getVendorId(), station.getVendorId(), transInfo.getStationId());
                    return new ResultResponse("0003", "No permission to operate on another vendor's station: " + transInfo.getStationId());
                }
                
                CarbonCharger charger = null;
                // Check if charger and connector exist
                if (StringUtils.hasText(transInfo.getConnectId())) {
                    charger = chargerRepository.findByChargerIdAndConnectId(
                            transInfo.getChargerId(), transInfo.getConnectId());
                    
                    if (charger == null) {
                        logger.error("Charger or connector does not exist, chargerId: {}, connectId: {}", 
                                transInfo.getChargerId(), transInfo.getConnectId());
                        return new ResultResponse("0005", "Charger or connector does not exist");
                    }
                    
                    // Check if charger belongs to requesting vendor or user is admin
                    if (!charger.getVendorId().equals(request.getVendorId()) && !authorizationService.isCurrentUserAdmin()) {
                        logger.error("Charger does not belong to requesting vendor, requestVendorId: {}, chargerVendorId: {}, chargerId: {}", 
                                request.getVendorId(), charger.getVendorId(), transInfo.getChargerId());
                        return new ResultResponse("0003", "No permission to operate on another vendor's charger: " + transInfo.getChargerId());
                    }
                }
                
                // Validate carbonCredit field value
                if (StringUtils.hasText(transInfo.getCarbonCredit()) && 
                        !("0".equals(transInfo.getCarbonCredit()) || "1".equals(transInfo.getCarbonCredit()))) {
                    logger.error("Invalid carbon credit status value, carbonCredit: {}, must be 0 or 1", transInfo.getCarbonCredit());
                    return new ResultResponse("0007", "Invalid carbon credit status value, must be 0(disagree) or 1(agree)");
                }
                
                // Check if transaction record already exists
                CarbonCharge charge = chargeRepository.findByTransNo(transInfo.getTransNo());
                
                if (charge != null) {
                    // Record exists, update not allowed
                    logger.warn("Transaction record already exists, update not allowed, transNo: {}", transInfo.getTransNo());
                    return new ResultResponse("0006", "Transaction record already exists, update not allowed: " + transInfo.getTransNo());
                }
                
                // Add new record
                logger.debug("Adding new transaction record, transNo: {}", transInfo.getTransNo());
                charge = new CarbonCharge();
                charge.setTransNo(transInfo.getTransNo());
                charge.setVendorId(request.getVendorId());
                charge.setStationId(transInfo.getStationId());
                charge.setChargerId(transInfo.getChargerId());
                charge.setConnectId(transInfo.getConnectId());
                charge.setInvoiceNo(transInfo.getInvoiceNo());
                
                // Set new fields
                charge.setCustomerId(transInfo.getCustomerId());
                charge.setInvoiceDate(transInfo.getInvoiceDate());
                charge.setCarbonCredit(transInfo.getCarbonCredit());
                
                charge.setStartTime(transInfo.getStartTime());
                charge.setEndTime(transInfo.getEndTime());
                charge.setKwh(transInfo.getKwh());
                charge.setCarType(transInfo.getCarType());
                charge.setCarTonnage(transInfo.getCarTonnage());
                charge.setCarNo(transInfo.getCarNo());
                charge.setCreateTime(LocalDateTime.now().format(formatter));
                charge.setUpdateTime(LocalDateTime.now().format(formatter));
                
                // Save
                chargeRepository.save(charge);

                // 自動更新該槍的首次交易時間 (以 firstEnableDate 起算)
                updateChargerFirstTransactionTime(charger, charge.getStartTime());
                
                logger.debug("Transaction record saved successfully, transNo: {}, customerId: {}, carbonCredit: {}", 
                        transInfo.getTransNo(), transInfo.getCustomerId(), transInfo.getCarbonCredit());
            }
            
            logger.debug("Charge information update completed");
            return ResultResponse.success();
        } catch (Exception e) {
            logger.error("Error occurred when updating charge information", e);
            return ResultResponse.fail();
        }
    }

    /**
     * 自動判定並更新充電樁槍頭的首次交易時間
     * 規則：交易時間必須大於等於 firstEnableDate (00:00:00)
     */
    private void updateChargerFirstTransactionTime(CarbonCharger charger, String transStartTime) {
        if (charger == null || !StringUtils.hasText(transStartTime)) {
            return;
        }

        // 檢查是否大於等於首次啟用日期
        String enableDate = charger.getFirstEnableDate();
        if (StringUtils.hasText(enableDate)) {
            String minTime = enableDate.trim();
            if (minTime.length() == 10) {
                minTime += " 00:00:00";
            }
            if (transStartTime.compareTo(minTime) < 0) {
                // 交易時間早於啟用日期，不視為有效首筆交易
                return;
            }
        }

        String currentFirstTx = charger.getFirstTransactionTime();
        if (!StringUtils.hasText(currentFirstTx)) {
            charger.setFirstTransactionTime(transStartTime);
            charger.setUpdateTime(LocalDateTime.now().format(formatter));
            chargerRepository.save(charger);
            logger.info("Auto set firstTransactionTime for charger {} connect {}: {}", 
                    charger.getChargerId(), charger.getConnectId(), transStartTime);
        } else if (transStartTime.compareTo(currentFirstTx) < 0) {
            charger.setFirstTransactionTime(transStartTime);
            charger.setUpdateTime(LocalDateTime.now().format(formatter));
            chargerRepository.save(charger);
            logger.info("Calibrated earlier firstTransactionTime for charger {} connect {}: {} -> {}", 
                    charger.getChargerId(), charger.getConnectId(), currentFirstTx, transStartTime);
        }
    }

    @Override
    public List<CarbonCharge> getChargeInfo(String vendorId, String stationId, String chargerId) {
        logger.debug("Getting charge information, vendorId: {}, stationId: {}, chargerId: {}", vendorId, stationId, chargerId);
        
        // Query charge information based on conditions
        if (vendorId != null && stationId != null && chargerId != null) {
            // Query transactions for specific vendor, station and charger
            return chargeRepository.findByVendorIdAndStationIdAndChargerId(vendorId, stationId, chargerId);
        } else if (vendorId != null && stationId != null) {
            // Query transactions for specific vendor and station
            return chargeRepository.findByVendorIdAndStationId(vendorId, stationId);
        } else if (vendorId != null) {
            // Query all transactions for specific vendor
            return chargeRepository.findByVendorId(vendorId);
        } else if (stationId != null) {
            // Query all transactions for specific station
            return chargeRepository.findByStationId(stationId);
        } else {
            // If no conditions specified, return all transactions
            return chargeRepository.findAll();
        }
    }

    @Override
    public List<CarbonCharge> getChargeInfoWithTimeRange(String vendorId, String stationId, String chargerId, 
                                                      String startTimeFrom, String startTimeTo) {
        logger.debug("Getting charge information with time range, vendorId: {}, stationId: {}, chargerId: {}, startTimeFrom: {}, startTimeTo: {}", 
                vendorId, stationId, chargerId, startTimeFrom, startTimeTo);
        
        // 同时指定开始时间和结束时间
        if (startTimeFrom != null && startTimeTo != null) {
            if (vendorId != null && stationId != null && chargerId != null) {
                return chargeRepository.findByVendorIdAndStationIdAndChargerIdAndStartTimeBetween(
                        vendorId, stationId, chargerId, startTimeFrom, startTimeTo);
            } else if (vendorId != null && stationId != null) {
                return chargeRepository.findByVendorIdAndStationIdAndStartTimeBetween(
                        vendorId, stationId, startTimeFrom, startTimeTo);
            } else if (vendorId != null) {
                return chargeRepository.findByVendorIdAndStartTimeBetween(vendorId, startTimeFrom, startTimeTo);
            } else {
                return chargeRepository.findByStartTimeBetween(startTimeFrom, startTimeTo);
            }
        } 
        // 只指定开始时间下限
        else if (startTimeFrom != null) {
            if (vendorId != null) {
                return chargeRepository.findByVendorIdAndStartTimeGreaterThanEqual(vendorId, startTimeFrom);
            } else {
                return chargeRepository.findByStartTimeGreaterThanEqual(startTimeFrom);
            }
        } 
        // 只指定开始时间上限
        else if (startTimeTo != null) {
            if (vendorId != null) {
                return chargeRepository.findByVendorIdAndStartTimeLessThanEqual(vendorId, startTimeTo);
            } else {
                return chargeRepository.findByStartTimeLessThanEqual(startTimeTo);
            }
        } 
        // 没有指定时间范围
        else {
            return getChargeInfo(vendorId, stationId, chargerId);
        }
    }

    @Override
    public CarbonCharge getChargeInfoByTransNo(String transNo) {
        logger.debug("Getting charge information by transNo: {}", transNo);
        return chargeRepository.findByTransNo(transNo);
    }
} 