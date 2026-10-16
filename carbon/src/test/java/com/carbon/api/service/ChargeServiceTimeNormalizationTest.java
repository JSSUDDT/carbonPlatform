package com.carbon.api.service;

import com.carbon.api.dto.ChargeRequest;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.repository.ChargerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.impl.ChargeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChargeServiceTimeNormalizationTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private ChargerRepository chargerRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(authorizationService.hasAccessToVendor(any())).thenReturn(true);
        when(authorizationService.isCurrentUserAdmin()).thenReturn(false);

        // Default mock for station ST001 owned by linbros
        CarbonStation stationLinbros = new CarbonStation();
        stationLinbros.setStationId("ST001");
        stationLinbros.setVendorId("linbros");
        when(stationRepository.findByStationId("ST001")).thenReturn(stationLinbros);

        // Default mock for station ST002 owned by EVRun
        CarbonStation stationEVRun = new CarbonStation();
        stationEVRun.setStationId("ST002");
        stationEVRun.setVendorId("EVRun");
        when(stationRepository.findByStationId("ST002")).thenReturn(stationEVRun);
    }

    @Test
    public void testLinbrosDateNormalization_10Chars() {
        // Arrange: linbros uploads 10-char date "2026-10-16"
        ChargeRequest request = new ChargeRequest();
        request.setVendorId("linbros");
        
        List<ChargeRequest.TransInfo> list = new ArrayList<>();
        ChargeRequest.TransInfo trans = new ChargeRequest.TransInfo();
        trans.setStationId("ST001");
        trans.setTransNo("TX1001");
        trans.setChargerId("CH001");
        trans.setConnectId("1");
        trans.setStartTime("2026-10-16");
        trans.setEndTime("2026-10-16");
        trans.setKwh("25.5");
        list.add(trans);
        request.setTransAry(list);

        CarbonCharger charger = new CarbonCharger("CH001", "1", "linbros", "ST001");
        when(chargerRepository.findByChargerIdAndConnectId("CH001", "1")).thenReturn(charger);

        // Act
        chargeService.updateChargeInfo(request);

        // Assert: 2026-10-16 in Taiwan (UTC+8) 00:00:00 -> 2026-10-15 16:00:00 UTC
        ArgumentCaptor<com.carbon.api.entity.CarbonCharge> captor = ArgumentCaptor.forClass(com.carbon.api.entity.CarbonCharge.class);
        verify(chargeRepository).save(captor.capture());

        com.carbon.api.entity.CarbonCharge saved = captor.getValue();
        assertEquals("2026-10-15 16:00:00", saved.getStartTime());
        assertEquals("2026-10-15 16:00:00", saved.getEndTime());
    }

    @Test
    public void testLinbrosDateTimeNormalization_19Chars() {
        // Arrange: linbros uploads 19-char datetime "2026-10-16 14:30:00"
        ChargeRequest request = new ChargeRequest();
        request.setVendorId("linbros");
        
        List<ChargeRequest.TransInfo> list = new ArrayList<>();
        ChargeRequest.TransInfo trans = new ChargeRequest.TransInfo();
        trans.setStationId("ST001");
        trans.setTransNo("TX1002");
        trans.setChargerId("CH001");
        trans.setConnectId("1");
        trans.setStartTime("2026-10-16 14:30:00");
        trans.setEndTime("2026-10-16 15:00:00");
        trans.setKwh("15.0");
        list.add(trans);
        request.setTransAry(list);

        CarbonCharger charger = new CarbonCharger("CH001", "1", "linbros", "ST001");
        when(chargerRepository.findByChargerIdAndConnectId("CH001", "1")).thenReturn(charger);

        // Act
        chargeService.updateChargeInfo(request);

        // Assert: 2026-10-16 14:30:00 (UTC+8) -> 2026-10-16 06:30:00 (UTC)
        ArgumentCaptor<com.carbon.api.entity.CarbonCharge> captor = ArgumentCaptor.forClass(com.carbon.api.entity.CarbonCharge.class);
        verify(chargeRepository).save(captor.capture());

        com.carbon.api.entity.CarbonCharge saved = captor.getValue();
        assertEquals("2026-10-16 06:30:00", saved.getStartTime());
        assertEquals("2026-10-16 07:00:00", saved.getEndTime());
    }

    @Test
    public void testOtherVendorTime_NotNormalized() {
        // Arrange: EVRun uploads UTC+0 datetime
        ChargeRequest request = new ChargeRequest();
        request.setVendorId("EVRun");
        
        List<ChargeRequest.TransInfo> list = new ArrayList<>();
        ChargeRequest.TransInfo trans = new ChargeRequest.TransInfo();
        trans.setStationId("ST002");
        trans.setTransNo("TX1003");
        trans.setChargerId("CH002");
        trans.setConnectId("1");
        trans.setStartTime("2026-10-16 06:30:00");
        trans.setEndTime("2026-10-16 07:00:00");
        trans.setKwh("30.0");
        list.add(trans);
        request.setTransAry(list);

        CarbonCharger charger = new CarbonCharger("CH002", "1", "EVRun", "ST002");
        when(chargerRepository.findByChargerIdAndConnectId("CH002", "1")).thenReturn(charger);

        // Act
        chargeService.updateChargeInfo(request);

        // Assert: EVRun keeps original UTC value
        ArgumentCaptor<com.carbon.api.entity.CarbonCharge> captor = ArgumentCaptor.forClass(com.carbon.api.entity.CarbonCharge.class);
        verify(chargeRepository).save(captor.capture());

        com.carbon.api.entity.CarbonCharge saved = captor.getValue();
        assertEquals("2026-10-16 06:30:00", saved.getStartTime());
        assertEquals("2026-10-16 07:00:00", saved.getEndTime());
    }

    @Test
    public void testFirstTransactionTimeWithEnableDate() {
        // Arrange: charger enabled on 2026-05-12 Taiwan date (= 2026-05-11 16:00:00 UTC)
        CarbonCharger charger = new CarbonCharger("CH001", "1", "linbros", "ST001");
        charger.setFirstEnableDate("2026-05-12");

        ChargeRequest request = new ChargeRequest();
        request.setVendorId("linbros");
        
        List<ChargeRequest.TransInfo> list = new ArrayList<>();
        ChargeRequest.TransInfo trans = new ChargeRequest.TransInfo();
        trans.setStationId("ST001");
        trans.setTransNo("TX1004");
        trans.setChargerId("CH001");
        trans.setConnectId("1");
        // Transaction on 2026-05-12 Taiwan date -> normalized to 2026-05-11 16:00:00 UTC
        trans.setStartTime("2026-05-12");
        trans.setEndTime("2026-05-12");
        trans.setKwh("10.0");
        list.add(trans);
        request.setTransAry(list);

        when(chargerRepository.findByChargerIdAndConnectId("CH001", "1")).thenReturn(charger);

        // Act
        chargeService.updateChargeInfo(request);

        // Assert: firstTransactionTime is set to 2026-05-11 16:00:00
        assertEquals("2026-05-11 16:00:00", charger.getFirstTransactionTime());
    }
}
