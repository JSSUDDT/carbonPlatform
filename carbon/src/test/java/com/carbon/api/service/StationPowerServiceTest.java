package com.carbon.api.service;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.dto.StationPowerRequest;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.CarbonStationPower;
import com.carbon.api.repository.StationPowerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.impl.StationPowerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StationPowerServiceTest {

    @Mock
    private StationPowerRepository stationPowerRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationPowerServiceImpl stationPowerService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TEST_VENDOR_ID = "VENDOR_TEST";
    private static final String TEST_STATION_ID = "STATION_TEST";
    private static final String TEST_DATE = "2023-05-01";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUpdateStationPower_Success() {
        // 模擬場站存在
        CarbonStation station = new CarbonStation();
        station.setVendorId(TEST_VENDOR_ID);
        station.setStationId(TEST_STATION_ID);
        when(stationRepository.findByStationIdAndVendorId(TEST_STATION_ID, TEST_VENDOR_ID)).thenReturn(station);
        
        // 模擬日期記錄不存在
        when(stationPowerRepository.findByVendorIdAndStationIdAndDateInfo(TEST_VENDOR_ID, TEST_STATION_ID, TEST_DATE))
                .thenReturn(null);

        // 創建請求
        StationPowerRequest request = new StationPowerRequest();
        request.setVendorId(TEST_VENDOR_ID);
        request.setStationId(TEST_STATION_ID);
        request.setStationKwh("1000");
        
        List<StationPowerRequest.DailyPowerInfo> dailyInfoList = new ArrayList<>();
        StationPowerRequest.DailyPowerInfo dailyInfo = new StationPowerRequest.DailyPowerInfo();
        dailyInfo.setDateInfo(TEST_DATE);
        dailyInfo.setDateKwh("100");
        dailyInfoList.add(dailyInfo);
        
        request.setEveryDayKwh(dailyInfoList);

        // 執行測試
        ResultResponse response = stationPowerService.updateStationPower(request);

        // 驗證結果
        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        
        // 驗證保存方法被調用
        verify(stationPowerRepository, times(1)).save(any(CarbonStationPower.class));
        verify(stationRepository, times(1)).save(any(CarbonStation.class));
    }

    @Test
    public void testUpdateStationPower_DuplicateDates() {
        // 模擬場站存在
        CarbonStation station = new CarbonStation();
        station.setVendorId(TEST_VENDOR_ID);
        station.setStationId(TEST_STATION_ID);
        when(stationRepository.findByStationIdAndVendorId(TEST_STATION_ID, TEST_VENDOR_ID)).thenReturn(station);

        // 創建請求 - 包含重複日期
        StationPowerRequest request = new StationPowerRequest();
        request.setVendorId(TEST_VENDOR_ID);
        request.setStationId(TEST_STATION_ID);
        request.setStationKwh("1000");
        
        List<StationPowerRequest.DailyPowerInfo> dailyInfoList = new ArrayList<>();
        
        StationPowerRequest.DailyPowerInfo dailyInfo1 = new StationPowerRequest.DailyPowerInfo();
        dailyInfo1.setDateInfo(TEST_DATE);
        dailyInfo1.setDateKwh("100");
        dailyInfoList.add(dailyInfo1);
        
        StationPowerRequest.DailyPowerInfo dailyInfo2 = new StationPowerRequest.DailyPowerInfo();
        dailyInfo2.setDateInfo(TEST_DATE); // 重複的日期
        dailyInfo2.setDateKwh("200");
        dailyInfoList.add(dailyInfo2);
        
        request.setEveryDayKwh(dailyInfoList);

        // 執行測試
        ResultResponse response = stationPowerService.updateStationPower(request);

        // 驗證結果
        assertNotNull(response);
        assertEquals("0003", response.getResultCode());
        
        // 驗證不會保存任何記錄
        verify(stationPowerRepository, never()).save(any(CarbonStationPower.class));
    }

    @Test
    public void testUpdateStationPower_DateRecordAlreadyExists() {
        // 模擬場站存在
        CarbonStation station = new CarbonStation();
        station.setVendorId(TEST_VENDOR_ID);
        station.setStationId(TEST_STATION_ID);
        when(stationRepository.findByStationIdAndVendorId(TEST_STATION_ID, TEST_VENDOR_ID)).thenReturn(station);
        
        // 模擬日期記錄已存在
        CarbonStationPower existingPower = new CarbonStationPower();
        existingPower.setVendorId(TEST_VENDOR_ID);
        existingPower.setStationId(TEST_STATION_ID);
        existingPower.setDateInfo(TEST_DATE);
        when(stationPowerRepository.findByVendorIdAndStationIdAndDateInfo(TEST_VENDOR_ID, TEST_STATION_ID, TEST_DATE))
                .thenReturn(existingPower);

        // 創建請求
        StationPowerRequest request = new StationPowerRequest();
        request.setVendorId(TEST_VENDOR_ID);
        request.setStationId(TEST_STATION_ID);
        request.setStationKwh("1000");
        
        List<StationPowerRequest.DailyPowerInfo> dailyInfoList = new ArrayList<>();
        StationPowerRequest.DailyPowerInfo dailyInfo = new StationPowerRequest.DailyPowerInfo();
        dailyInfo.setDateInfo(TEST_DATE);
        dailyInfo.setDateKwh("100");
        dailyInfoList.add(dailyInfo);
        
        request.setEveryDayKwh(dailyInfoList);

        // 執行測試
        ResultResponse response = stationPowerService.updateStationPower(request);

        // 驗證結果
        assertNotNull(response);
        assertEquals("0002", response.getResultCode());
        
        // 驗證不會保存新的記錄
        verify(stationPowerRepository, never()).save(any(CarbonStationPower.class));
    }

    @Test
    public void testUpdateStationPower_StationNotFound() {
        // 模擬場站不存在
        when(stationRepository.findByStationIdAndVendorId(TEST_STATION_ID, TEST_VENDOR_ID)).thenReturn(null);

        // 創建請求
        StationPowerRequest request = new StationPowerRequest();
        request.setVendorId(TEST_VENDOR_ID);
        request.setStationId(TEST_STATION_ID);
        request.setStationKwh("1000");

        // 執行測試
        ResultResponse response = stationPowerService.updateStationPower(request);

        // 驗證結果
        assertNotNull(response);
        assertEquals("0001", response.getResultCode());
        
        // 驗證不會保存任何記錄
        verify(stationPowerRepository, never()).save(any(CarbonStationPower.class));
        verify(stationRepository, never()).save(any(CarbonStation.class));
    }
} 