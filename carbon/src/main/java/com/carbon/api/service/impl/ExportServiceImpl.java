package com.carbon.api.service.impl;

import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.service.ExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private StationRepository stationRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 取得多語言欄位名稱
     */
    private String[] getColumnHeaders(String language) {
        if ("en".equals(language)) {
            return new String[]{
                    "Transaction No", "Vendor ID", "Station ID", "Station Name",
                    "Charger ID", "Connect ID", "Invoice No", "Customer ID",
                    "Invoice Date", "Start Time", "End Time", "Charging (kWh)",
                    "Car No", "Upload Time"
            };
        } else {
            return new String[]{
                    "交易號", "廠商ID", "充電站ID", "充電站名稱",
                    "充電樁ID", "連接ID", "發票號", "客戶ID",
                    "發票日期", "開始時間", "結束時間", "充電量(kWh)",
                    "車輛號碼", "上傳時間"
            };
        }
    }

    /**
     * 將 UTC+0 時間轉換為台灣時間 (UTC+8)
     */
    private String convertToTaiwanTime(String utcTimeString) {
        if (utcTimeString == null || utcTimeString.trim().isEmpty()) {
            return "";
        }

        try {
            LocalDateTime utcDateTime = LocalDateTime.parse(utcTimeString, formatter);
            ZonedDateTime utcZoned = utcDateTime.atZone(ZoneId.of("UTC"));
            ZonedDateTime taiwanZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Taipei"));
            return taiwanZoned.format(formatter);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse time string: {}, returning original string", utcTimeString);
            return utcTimeString;
        } catch (Exception e) {
            logger.error("Error converting time to Taiwan time: {}", e.getMessage());
            return utcTimeString;
        }
    }

    /**
     * 取得充電記錄資料
     */
    private List<CarbonCharge> getChargeData(String vendorId, String stationId, 
                                               String startTimeFrom, String startTimeTo) {
        logger.info("========== 開始查詢充電記錄 ==========");
        logger.info("vendorId: {}", vendorId);
        logger.info("stationId: {}", stationId);
        logger.info("startTimeFrom: {}", startTimeFrom);
        logger.info("startTimeTo: {}", startTimeTo);

        List<CarbonCharge> charges;

        // 管理員（vendorId 為 null）可以查詢所有資料
        if (vendorId == null) {
            logger.info("管理員查詢，取得所有充電記錄");
            if (stationId != null && !stationId.isEmpty() && 
                startTimeFrom != null && !startTimeFrom.isEmpty() && 
                startTimeTo != null && !startTimeTo.isEmpty()) {
                charges = chargeRepository.findByStationIdAndStartTimeBetween(stationId, startTimeFrom, startTimeTo);
            } else if (stationId != null && !stationId.isEmpty()) {
                charges = chargeRepository.findByStationId(stationId);
            } else if (startTimeFrom != null && !startTimeFrom.isEmpty() && 
                       startTimeTo != null && !startTimeTo.isEmpty()) {
                charges = chargeRepository.findByStartTimeBetween(startTimeFrom, startTimeTo);
            } else {
                charges = chargeRepository.findAll();
            }
        } else {
            // 普通用戶只能查詢自己的資料
            logger.info("普通用戶查詢，vendorId: {}", vendorId);
            if (stationId != null && !stationId.isEmpty() && 
                startTimeFrom != null && !startTimeFrom.isEmpty() && 
                startTimeTo != null && !startTimeTo.isEmpty()) {
                logger.info("使用查詢：findByVendorIdAndStationIdAndStartTimeBetween");
                charges = chargeRepository.findByVendorIdAndStationIdAndStartTimeBetween(
                        vendorId, stationId, startTimeFrom, startTimeTo);
            } else if (stationId != null && !stationId.isEmpty()) {
                logger.info("使用查詢：findByVendorIdAndStationId");
                charges = chargeRepository.findByVendorIdAndStationId(vendorId, stationId);
            } else if (startTimeFrom != null && !startTimeFrom.isEmpty() && 
                       startTimeTo != null && !startTimeTo.isEmpty()) {
                logger.info("使用查詢：findByVendorIdAndStartTimeBetween");
                charges = chargeRepository.findByVendorIdAndStartTimeBetween(
                        vendorId, startTimeFrom, startTimeTo);
                logger.info("查詢SQL執行完成，結果數量: {}", charges.size());
                
                // 如果沒有資料，嘗試查詢所有該 vendor 的資料來診斷問題
                if (charges.isEmpty()) {
                    logger.warn("時間範圍查詢無結果，嘗試查詢該 vendor 的所有資料");
                    List<CarbonCharge> allCharges = chargeRepository.findByVendorId(vendorId);
                    logger.info("該 vendor 總共有 {} 筆資料", allCharges.size());
                    if (!allCharges.isEmpty()) {
                        logger.info("第一筆資料的 startTime: {}", allCharges.get(0).getStartTime());
                        logger.info("最後一筆資料的 startTime: {}", allCharges.get(allCharges.size()-1).getStartTime());
                    }
                }
            } else {
                logger.info("使用查詢：findByVendorId");
                charges = chargeRepository.findByVendorId(vendorId);
            }
        }

        logger.info("========== 查詢結果：{} 筆充電記錄 ==========", charges.size());
        if (charges.size() > 0) {
            logger.info("第一筆資料 transNo: {}", charges.get(0).getTransNo());
            logger.info("最後一筆資料 transNo: {}", charges.get(charges.size() - 1).getTransNo());
        }
        return charges;
    }

    /**
     * 建立充電站名稱映射
     */
    private Map<String, String> buildStationNameMap(List<CarbonCharge> charges) {
        Map<String, String> stationNameMap = new HashMap<>();
        charges.forEach(charge -> {
            if (!stationNameMap.containsKey(charge.getStationId())) {
                CarbonStation station = stationRepository.findByStationId(charge.getStationId());
                if (station != null && station.getStationName() != null) {
                    stationNameMap.put(charge.getStationId(), station.getStationName());
                } else {
                    stationNameMap.put(charge.getStationId(), charge.getStationId());
                }
            }
        });
        return stationNameMap;
    }

    @Override
    public ByteArrayOutputStream exportChargeInfoToExcel(String vendorId, String stationId, 
                                                          String startTimeFrom, String startTimeTo, 
                                                          String language) throws Exception {
        logger.info("開始匯出 Excel，vendorId: {}, language: {}", vendorId, language);

        // 取得資料
        List<CarbonCharge> charges = getChargeData(vendorId, stationId, startTimeFrom, startTimeTo);
        Map<String, String> stationNameMap = buildStationNameMap(charges);

        // 建立工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("en".equals(language) ? "Charging Records" : "充電記錄");

        // 建立樣式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 建立表頭
        Row headerRow = sheet.createRow(0);
        String[] headers = getColumnHeaders(language);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充資料
        int rowNum = 1;
        for (CarbonCharge charge : charges) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            String stationName = stationNameMap.getOrDefault(charge.getStationId(), charge.getStationId());

            Cell cell0 = row.createCell(colNum++);
            cell0.setCellValue(charge.getTransNo() != null ? charge.getTransNo() : "");
            cell0.setCellStyle(dataStyle);

            Cell cell1 = row.createCell(colNum++);
            cell1.setCellValue(charge.getVendorId() != null ? charge.getVendorId() : "");
            cell1.setCellStyle(dataStyle);

            Cell cell2 = row.createCell(colNum++);
            cell2.setCellValue(charge.getStationId() != null ? charge.getStationId() : "");
            cell2.setCellStyle(dataStyle);

            Cell cell3 = row.createCell(colNum++);
            cell3.setCellValue(stationName);
            cell3.setCellStyle(dataStyle);

            Cell cell4 = row.createCell(colNum++);
            cell4.setCellValue(charge.getChargerId() != null ? charge.getChargerId() : "");
            cell4.setCellStyle(dataStyle);

            Cell cell5 = row.createCell(colNum++);
            cell5.setCellValue(charge.getConnectId() != null ? charge.getConnectId() : "");
            cell5.setCellStyle(dataStyle);

            Cell cell6 = row.createCell(colNum++);
            cell6.setCellValue(charge.getInvoiceNo() != null ? charge.getInvoiceNo() : "");
            cell6.setCellStyle(dataStyle);

            Cell cell7 = row.createCell(colNum++);
            cell7.setCellValue(charge.getCustomerId() != null ? charge.getCustomerId() : "");
            cell7.setCellStyle(dataStyle);

            Cell cell8 = row.createCell(colNum++);
            cell8.setCellValue(charge.getInvoiceDate() != null ? charge.getInvoiceDate() : "");
            cell8.setCellStyle(dataStyle);

            Cell cell9 = row.createCell(colNum++);
            cell9.setCellValue(convertToTaiwanTime(charge.getStartTime()));
            cell9.setCellStyle(dataStyle);

            Cell cell10 = row.createCell(colNum++);
            cell10.setCellValue(convertToTaiwanTime(charge.getEndTime()));
            cell10.setCellStyle(dataStyle);

            Cell cell11 = row.createCell(colNum++);
            cell11.setCellValue(charge.getKwh() != null ? charge.getKwh() : "");
            cell11.setCellStyle(dataStyle);

            Cell cell12 = row.createCell(colNum++);
            cell12.setCellValue(charge.getCarNo() != null ? charge.getCarNo() : "");
            cell12.setCellStyle(dataStyle);

            Cell cell13 = row.createCell(colNum++);
            cell13.setCellValue(charge.getCreateTime() != null ? charge.getCreateTime() : "");
            cell13.setCellStyle(dataStyle);
        }

        // 自動調整欄寬
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 輸出到位元組陣列
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Excel 匯出完成，共 {} 筆資料", charges.size());
        return outputStream;
    }

}

