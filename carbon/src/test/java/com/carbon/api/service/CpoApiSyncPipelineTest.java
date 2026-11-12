package com.carbon.api.service;

import com.carbon.api.dto.SyncResult;
import com.carbon.api.dto.external.cpo.*;
import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.repository.ChargerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.repository.VendorRepository;
import com.carbon.api.service.impl.DataSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CpoApiSyncPipelineTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private ChargerRepository chargerRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private CpoApiClientService cpoApiClientService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSyncServiceImpl dataSyncService;

    private final String VENDOR_ID = "TEST_CPO_EV";
    private final String API_URL = "http://localhost:9095/carbon/api/test/mock-cpo/dataset";
    private final String TOKEN = "mock-secret-token";

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password_123");
        lenient().when(geocodingService.getCountryCode(anyString(), anyString())).thenReturn("TW");
    }

    @Test
    @DisplayName("測試 1: 首次同步時自動建立廠商帳號 (Auto-Provisioning)")
    void testAutoProvisionVendor() {
        when(vendorRepository.findByVendorId(VENDOR_ID)).thenReturn(null);

        dataSyncService.ensureVendorExists(VENDOR_ID);

        verify(vendorRepository, times(1)).save(argThat(vendor -> 
            VENDOR_ID.equals(vendor.getVendorId()) &&
            "1".equals(vendor.getStatus()) &&
            "ROLE_USER".equals(vendor.getRole())
        ));
    }

    @Test
    @DisplayName("測試 2: 完整 CPO 數據同步與三層級聯入庫流水線")
    void testFullCpoSyncPipeline() {
        // Arrange
        when(vendorRepository.findByVendorId(VENDOR_ID)).thenReturn(new CarbonVendor());

        CpoStationDto st = CpoStationDto.builder()
                .stationId("STA_01")
                .stationName("測試站點A")
                .gpsLatitude("25.0339")
                .gpsLongitude("121.5645")
                .address("台北市信義區")
                .build();

        CpoChargerDto ch = CpoChargerDto.builder()
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .firstEnableDate("2025-01-01")
                .chargerType("DC")
                .chargerKw("180")
                .build();

        // 正常交易
        CpoTransactionDto txValid = CpoTransactionDto.builder()
                .transNo("TX_VALID_01")
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .startTime("2026-11-01 10:00:00")
                .endTime("2026-11-01 10:45:00")
                .kwh(35.5)
                .carType("純電")
                .carNo("EAB-1234")
                .build();

        // 重複交易 (已存在於資料庫)
        CpoTransactionDto txDuplicate = CpoTransactionDto.builder()
                .transNo("TX_DUP_02")
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .startTime("2026-11-01 11:00:00")
                .endTime("2026-11-01 11:30:00")
                .kwh(20.0)
                .build();

        // 異常超長時長 (> 72小時)
        CpoTransactionDto txTooLong = CpoTransactionDto.builder()
                .transNo("TX_LONG_03")
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .startTime("2026-11-01 10:00:00")
                .endTime("2026-11-05 10:00:00")
                .kwh(50.0)
                .build();

        // 異常超大度數 (> 1000 kWh)
        CpoTransactionDto txAbnormalKwh = CpoTransactionDto.builder()
                .transNo("TX_KWH_04")
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .startTime("2026-11-01 12:00:00")
                .endTime("2026-11-01 12:30:00")
                .kwh(1500.0)
                .build();

        // 早於設備首次啟用日 (firstEnableDate: 2025-01-01)
        CpoTransactionDto txBeforeEnableDate = CpoTransactionDto.builder()
                .transNo("TX_BEFORE_05")
                .stationId("STA_01")
                .chargerId("EVSE_01")
                .connectId("1")
                .startTime("2024-12-31 10:00:00")
                .endTime("2024-12-31 10:30:00")
                .kwh(25.0)
                .build();

        CpoDatasetData datasetData = CpoDatasetData.builder()
                .stations(Collections.singletonList(st))
                .chargers(Collections.singletonList(ch))
                .transactions(Arrays.asList(txValid, txDuplicate, txTooLong, txAbnormalKwh, txBeforeEnableDate))
                .build();

        when(cpoApiClientService.fetchAllPages(eq(API_URL), eq(TOKEN), eq(VENDOR_ID), anyString(), anyString(), anyInt()))
                .thenReturn(datasetData);

        when(stationRepository.findByVendorIdAndStationId(VENDOR_ID, "STA_01")).thenReturn(null);

        CarbonCharger existingCharger = new CarbonCharger();
        existingCharger.setVendorId(VENDOR_ID);
        existingCharger.setStationId("STA_01");
        existingCharger.setChargerId("EVSE_01");
        existingCharger.setConnectId("1");
        existingCharger.setFirstEnableDate("2025-01-01");
        when(chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(VENDOR_ID, "STA_01", "EVSE_01", "1"))
                .thenReturn(existingCharger);

        // 重複交易在 DB 存在
        CarbonCharge dupCharge = new CarbonCharge();
        dupCharge.setTransNo("TX_DUP_02");
        when(chargeRepository.findByTransNo("TX_DUP_02")).thenReturn(dupCharge);
        when(chargeRepository.findByTransNo("TX_VALID_01")).thenReturn(null);

        // Act
        SyncResult result = dataSyncService.syncFromCpoApi(API_URL, TOKEN, VENDOR_ID, "2026-11-01 00:00:00", "2026-11-01 23:59:59");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(5, result.getTotalRecords());
        assertEquals(1, result.getSyncedRecords(), "僅有 1 筆完全合規之交易應被存檔入庫");
        assertEquals(4, result.getSkippedRecords(), "其餘 4 筆（重複、超時長、超度數、早於啟用日）應被略過");

        // 驗證站點儲存
        verify(stationRepository, times(1)).save(any(CarbonStation.class));

        // 驗證交易儲存 (僅 TX_VALID_01)
        verify(chargeRepository, times(1)).save(argThat(charge -> "TX_VALID_01".equals(charge.getTransNo())));
    }
}
