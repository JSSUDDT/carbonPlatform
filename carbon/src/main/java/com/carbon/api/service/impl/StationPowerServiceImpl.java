package com.carbon.api.service.impl;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.StationPowerRequest;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.CarbonStationPower;
import com.carbon.api.repository.StationPowerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.AuthorizationService;
import com.carbon.api.service.StationPowerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StationPowerServiceImpl implements StationPowerService {

    private static final Logger logger = LoggerFactory.getLogger(StationPowerServiceImpl.class);

    @Autowired
    private StationPowerRepository stationPowerRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private AuthorizationService authorizationService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public ResultResponse updateStationPower(StationPowerRequest request) {
        try {
            // 檢查請求是否為null
            if (request == null) {
                logger.error("請求對象為null");
                return new ResultResponse("0001", "請求數據無效");
            }
            
            // 檢查權限 - 確保調用者有權訪問該廠商的資源
            if (!authorizationService.hasAccessToVendor(request.getVendorId())) {
                logger.error("用戶無權訪問此廠商資源: {}", request.getVendorId());
                return new ResultResponse("0003", "無權訪問此廠商資源");
            }
            
            logger.info("收到場站負載信息更新請求，vendorId: [{}], stationId: [{}], stationKwh: [{}]", 
                    request.getVendorId(), request.getStationId(), request.getStationKwh());
            
            // 檢查請求參數
            if (request.getVendorId() == null || request.getVendorId().trim().isEmpty()) {
                logger.error("請求中vendorId為空");
                return new ResultResponse("0001", "廠商ID不能為空");
            }
            
            if (request.getStationId() == null || request.getStationId().trim().isEmpty()) {
                logger.error("請求中stationId為空");
                return new ResultResponse("0001", "場站ID不能為空");
            }
            
            // 檢查場站是否存在
            CarbonStation station = stationRepository.findByStationId(request.getStationId());
            if (station == null) {
                logger.error("未找到對應場站 stationId: [{}]", request.getStationId());
                return new ResultResponse("0004", "場站不存在: " + request.getStationId());
            }
            
            // 檢查場站是否屬於請求的廠商
            if (!station.getVendorId().equals(request.getVendorId()) && !authorizationService.isCurrentUserAdmin()) {
                logger.error("場站不屬於請求的廠商, 請求廠商: [{}], 場站廠商: [{}], stationId: [{}]", 
                        request.getVendorId(), station.getVendorId(), request.getStationId());
                return new ResultResponse("0003", "無權操作其他廠商的場站: " + request.getStationId());
            }
            
            // 更新場站總負載
            logger.info("找到場站記錄，更新場站總負載, stationId: [{}], stationKwh: [{}]", 
                    request.getStationId(), request.getStationKwh());
                    
            station.setStationKwh(request.getStationKwh());
            station.setUpdateTime(LocalDateTime.now().format(formatter));
            stationRepository.save(station);
            
            // 獲取每日負載數據列表，如果為null則初始化為空列表
            List<StationPowerRequest.DailyPowerInfo> dailyPowerInfoList = request.getEveryDayKwh();
            if (dailyPowerInfoList == null) {
                logger.warn("每日負載列表為null，stationId: [{}]", request.getStationId());
                dailyPowerInfoList = new ArrayList<>(); // 初始化為空列表避免NPE
            }
            
            logger.info("開始處理{}個每日負載信息", dailyPowerInfoList.size());
            
            // 檢查是否有重複的日期
            Set<String> dateInfoSet = new HashSet<>();
            for (StationPowerRequest.DailyPowerInfo dailyInfo : dailyPowerInfoList) {
                if (dailyInfo == null || dailyInfo.getDateInfo() == null) {
                    continue;
                }
                
                if (!dateInfoSet.add(dailyInfo.getDateInfo())) {
                    logger.warn("請求中包含重複的日期: [{}]", dailyInfo.getDateInfo());
                    return new ResultResponse("0003", "請求中包含重複的日期: " + dailyInfo.getDateInfo());
                }
            }
            
            // 檢查每個日期記錄是否已存在
            List<String> existingDates = new ArrayList<>();
            for (StationPowerRequest.DailyPowerInfo dailyInfo : dailyPowerInfoList) {
                if (dailyInfo == null || dailyInfo.getDateInfo() == null) {
                    continue;
                }
                
                logger.info("檢查日期記錄是否存在: vendorId=[{}], stationId=[{}], dateInfo=[{}]", 
                        request.getVendorId(), request.getStationId(), dailyInfo.getDateInfo());
                        
                // 檢查該日期記錄是否已存在
                CarbonStationPower existingPower = stationPowerRepository.findByVendorIdAndStationIdAndDateInfo(
                        request.getVendorId(), request.getStationId(), dailyInfo.getDateInfo());
                
                if (existingPower != null) {
                    logger.warn("該日期的記錄已存在，不允許更新: [{}]", dailyInfo.getDateInfo());
                    existingDates.add(dailyInfo.getDateInfo());
                }
            }
            
            // 如果有已存在的日期記錄，返回錯誤
            if (!existingDates.isEmpty()) {
                logger.warn("發現{}個已存在的日期記錄: {}", existingDates.size(), String.join(", ", existingDates));
                return new ResultResponse("0002", "以下日期的記錄已存在，不允許更新: " + String.join(", ", existingDates));
            }
            
            // 保存每日負載數據
            for (StationPowerRequest.DailyPowerInfo dailyInfo : dailyPowerInfoList) {
                if (dailyInfo == null) {
                    logger.warn("跳過null的每日負載信息");
                    continue;
                }
                
                // 創建新記錄
                CarbonStationPower power = new CarbonStationPower();
                power.setCreateTime(LocalDateTime.now().format(formatter));
                
                // 設置屬性
                power.setVendorId(request.getVendorId());
                power.setStationId(request.getStationId());
                power.setStationKwh(request.getStationKwh()); // 同時保存場站總負載
                power.setDateInfo(dailyInfo.getDateInfo());
                power.setDateKwh(dailyInfo.getDateKwh());
                power.setUpdateTime(LocalDateTime.now().format(formatter));
                
                // 保存
                stationPowerRepository.save(power);
                logger.info("新增每日負載記錄, stationId: [{}], date: [{}], kwh: [{}]", 
                        request.getStationId(), dailyInfo.getDateInfo(), dailyInfo.getDateKwh());
            }
            
            // 如果每日負載列表為空，仍然需要保存場站總負載信息
            if (dailyPowerInfoList.isEmpty()) {
                logger.info("每日負載列表為空，創建一條僅包含場站總負載的記錄");
                
                // 用當前日期作為標識創建一條記錄
                LocalDateTime now = LocalDateTime.now();
                String todayDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                logger.info("檢查當天記錄是否已存在: [{}]", todayDate);
                // 檢查當天記錄是否已存在
                CarbonStationPower existingPower = stationPowerRepository.findByVendorIdAndStationIdAndDateInfo(
                        request.getVendorId(), request.getStationId(), todayDate);
                
                if (existingPower != null) {
                    logger.warn("當天的記錄已存在，不允許更新: [{}]", todayDate);
                    return new ResultResponse("0002", "當天的記錄已存在，不允許更新: " + todayDate);
                }
                
                // 創建新記錄
                CarbonStationPower power = new CarbonStationPower();
                power.setVendorId(request.getVendorId());
                power.setStationId(request.getStationId());
                power.setStationKwh(request.getStationKwh());
                power.setDateInfo(todayDate);
                power.setCreateTime(now.format(formatter));
                power.setUpdateTime(now.format(formatter));
                
                stationPowerRepository.save(power);
                logger.info("新增當天負載記錄, stationId: [{}], date: [{}], kwh: [{}]", 
                        request.getStationId(), todayDate, request.getStationKwh());
            }
            
            logger.info("場站負載信息更新完成");
            return ResultResponse.success();
        } catch (Exception e) {
            logger.error("更新場站負載信息時發生錯誤: {}", e.getMessage(), e);
            return ResultResponse.fail();
        }
    }

    @Override
    public List<CarbonStationPower> getStationPower(String vendorId, String stationId) {
        logger.debug("獲取場站負載信息，vendorId: {}, stationId: {}", vendorId, stationId);
        
        // 根據條件查詢場站負載信息
        if (vendorId != null && stationId != null) {
            // 查詢指定廠商和場站的負載信息
            return stationPowerRepository.findByVendorIdAndStationId(vendorId, stationId);
        } else if (vendorId != null) {
            // 查詢指定廠商的所有負載信息
            return stationPowerRepository.findByVendorId(vendorId);
        } else if (stationId != null) {
            // 查詢指定場站的所有負載信息
            return stationPowerRepository.findByStationId(stationId);
        } else {
            // 如果未指定條件，則返回全部負載信息
            return stationPowerRepository.findAll();
        }
    }
} 