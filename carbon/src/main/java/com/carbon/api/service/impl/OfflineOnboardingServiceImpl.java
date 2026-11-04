package com.carbon.api.service.impl;

import com.carbon.api.dto.ResultResponse;
import com.carbon.api.entity.CarbonCharge;
import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.entity.CarbonVendor;
import com.carbon.api.repository.ChargeRepository;
import com.carbon.api.repository.ChargerRepository;
import com.carbon.api.repository.StationRepository;
import com.carbon.api.repository.VendorRepository;
import com.carbon.api.service.AuthorizationService;
import com.carbon.api.service.OfflineOnboardingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OfflineOnboardingServiceImpl implements OfflineOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OfflineOnboardingServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerRepository chargerRepository;

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 生成包含清晰指示的離線一站式建置與填報範本
     */
    @Override
    public ByteArrayOutputStream generateTemplateExcel() throws Exception {
        Workbook workbook = new XSSFWorkbook();

        // 建立風格樣式
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle subtitleStyle = workbook.createCellStyle();
        Font subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleFont.setFontHeightInPoints((short) 11);
        subtitleStyle.setFont(subtitleFont);

        CellStyle sectionHeaderStyle = workbook.createCellStyle();
        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setFontHeightInPoints((short) 11);
        sectionFont.setColor(IndexedColors.WHITE.getIndex());
        sectionHeaderStyle.setFont(sectionFont);
        sectionHeaderStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle colHeaderStyle = workbook.createCellStyle();
        Font colHeaderFont = workbook.createFont();
        colHeaderFont.setBold(true);
        colHeaderFont.setColor(IndexedColors.WHITE.getIndex());
        colHeaderStyle.setFont(colHeaderFont);
        colHeaderStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(colHeaderStyle);

        CellStyle sampleStyle = workbook.createCellStyle();
        Font sampleFont = workbook.createFont();
        sampleFont.setItalic(true);
        sampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        sampleStyle.setFont(sampleFont);
        sampleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sampleStyle.setAlignment(HorizontalAlignment.CENTER);
        sampleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(sampleStyle);

        CellStyle normalTextStyle = workbook.createCellStyle();
        normalTextStyle.setWrapText(true);
        normalTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // ==========================================
        // 工作表 0：0_填寫說明 (Instructions)
        // ==========================================
        Sheet instructionSheet = workbook.createSheet("0_填寫說明");
        instructionSheet.setDisplayGridlines(true);

        int rowIdx = 0;
        Row rTitle = instructionSheet.createRow(rowIdx++);
        rTitle.setHeightInPoints(30);
        Cell cTitle = rTitle.createCell(0);
        cTitle.setCellValue("台灣電動車充電系統碳權平台 - 離線業者一站式建置與交易填報說明書 (Verra 5455)");
        cTitle.setCellStyle(titleStyle);

        Row rBlank = instructionSheet.createRow(rowIdx++);

        // 核心填報原則
        Row rSec1 = instructionSheet.createRow(rowIdx++);
        rSec1.setHeightInPoints(24);
        Cell cSec1 = rSec1.createCell(0);
        cSec1.setCellValue("【一、 核心填報原則】");
        cSec1.setCellStyle(sectionHeaderStyle);

        String[] principles = {
            "1. 本 Excel 專供離線或無法串接 API 之充電營運業者（CPO）進行「一站式開通與交易上傳」使用。",
            "2. 匯入後，系統將自動依序建立：業者帳號 (Sheet 1) ➔ 場站 (Sheet 2) ➔ 充電槍頭 (Sheet 3) ➔ 寫入充電交易 (Sheet 4)。",
            "3. 所有標註星號「*」之欄位皆為 API 官方文件明訂之必填欄位 (Mandatory)，不可留空或隨意刪減欄位。",
            "4. 各工作表中帶有「[範例]」開頭或淺灰底色之列為填寫示範，正式匯入時請替換或直接填寫實際營運資料（系統會自動略過標記為[範例]之測試列）。",
            "5. 匯入完成後，業者即可直接前往平台登入頁 (/login.html)，使用 Sheet 1 設定之帳號密碼登入，即時查驗自身名下之所有場站、槍頭與交易明細。"
        };
        for (String p : principles) {
            Row r = instructionSheet.createRow(rowIdx++);
            Cell c = r.createCell(0);
            c.setCellValue(p);
            c.setCellStyle(normalTextStyle);
        }

        rowIdx++;
        // 各工作表填寫指引
        Row rSec2 = instructionSheet.createRow(rowIdx++);
        rSec2.setHeightInPoints(24);
        Cell cSec2 = rSec2.createCell(0);
        cSec2.setCellValue("【二、 各工作表詳細填寫指南】");
        cSec2.setCellStyle(sectionHeaderStyle);

        String[] sheetGuides = {
            "■ Sheet 1【1_業者帳號】(對應 API-101)",
            "  • 僅需填寫 1 列資料。包含「業者代碼 (vendorId)」與「預設登入密碼 (password)」。",
            "  • 匯入時系統會自動加密存入資料庫，並設定為業者權限 (ROLE_USER)。",
            "",
            "■ Sheet 2【2_充電站資訊】(對應 API-102)",
            "  • 一個充電站據點填寫 1 列。包含場站代碼、名稱與 GPS 經緯度（經緯度為官方必填，供國際碳權查驗與地圖定位使用）。",
            "  • 場站狀態 (stationStatus) 系統將自動預設為啟用 ('1')，更新狀態 (updateStatus) 自動帶 'U'。",
            "",
            "■ Sheet 3【3_充電槍資訊】(對應 API-103) - ★重要設備結構",
            "  • 本平台以「槍頭 (Connector)」為計費與複合主鍵單位 (chargerId + connectId)。",
            "  • 【一站多樁填寫方式】：同站若有多座充電樁，每列填寫相同的場站代碼 (stationId)，搭配不同的充電樁編號 (chargerId)。",
            "  • 【一樁雙槍填寫方式】：同一座充電樁若有兩支充電槍，請「連續填寫兩列」，兩列使用「相同的 chargerId」，而充電槍號 (connectId) 分別填寫 '1' 與 '2'。",
            "  • 首次啟用時間 (firstEnableDate) 請填寫 YYYY-MM-DD（如 2025-01-01），不可留空。",
            "",
            "■ Sheet 4【4_充電交易明細】(對應 API-104)",
            "  • 每筆實際充電紀錄填寫 1 列。必須精準對應至 Sheet 3 定義之 stationId、chargerId 與 connectId。",
            "  • 交易序號 (transNo) 必須唯一。若該單號先前已存在系統中，系統將啟動防篡改機制，自動執行【重複略過 (代碼 0006)】，保障數據唯一性。",
            "  • 開始與結束時間請填寫台灣標準時間 (UTC+8)，格式為 YYYY-MM-DD HH:mm:ss，系統入庫時會自動轉換為國際標準 UTC+0 儲存。",
            "  • 充電度數 (kwh) 請填寫大於 0 之實質用電量（支援小數點後兩位，如 35.50）。"
        };
        for (String g : sheetGuides) {
            Row r = instructionSheet.createRow(rowIdx++);
            Cell c = r.createCell(0);
            c.setCellValue(g);
            c.setCellStyle(normalTextStyle);
        }

        instructionSheet.setColumnWidth(0, 32000);

        // ==========================================
        // 工作表 1：1_業者帳號 (API-101)
        // ==========================================
        Sheet s1 = workbook.createSheet("1_業者帳號");
        s1.setDisplayGridlines(true);
        Row s1H = s1.createRow(0);
        s1H.setHeightInPoints(26);
        createHeaderCell(s1H, 0, "業者代碼 (vendorId) *", colHeaderStyle);
        createHeaderCell(s1H, 1, "預設登入密碼 (password) *", colHeaderStyle);

        Row s1D = s1.createRow(1);
        s1D.setHeightInPoints(22);
        createSampleCell(s1D, 0, "[範例] CPO_DEMO_01", sampleStyle);
        createSampleCell(s1D, 1, "[範例] Vendor@12345", sampleStyle);

        s1.setColumnWidth(0, 8000);
        s1.setColumnWidth(1, 8000);

        // ==========================================
        // 工作表 2：2_充電站資訊 (API-102)
        // ==========================================
        Sheet s2 = workbook.createSheet("2_充電站資訊");
        s2.setDisplayGridlines(true);
        Row s2H = s2.createRow(0);
        s2H.setHeightInPoints(26);
        createHeaderCell(s2H, 0, "場站代碼 (stationId) *", colHeaderStyle);
        createHeaderCell(s2H, 1, "場站名稱 (stationName) *", colHeaderStyle);
        createHeaderCell(s2H, 2, "GPS緯度 (gpsLatitude) *", colHeaderStyle);
        createHeaderCell(s2H, 3, "GPS經度 (gpsLongitude) *", colHeaderStyle);

        Row s2D1 = s2.createRow(1);
        s2D1.setHeightInPoints(22);
        createSampleCell(s2D1, 0, "[範例] ST_DEMO_01", sampleStyle);
        createSampleCell(s2D1, 1, "[範例] 內湖快充示範站", sampleStyle);
        createSampleCell(s2D1, 2, "25.0792", sampleStyle);
        createSampleCell(s2D1, 3, "121.5753", sampleStyle);

        s2.setColumnWidth(0, 7000);
        s2.setColumnWidth(1, 9000);
        s2.setColumnWidth(2, 6000);
        s2.setColumnWidth(3, 6000);

        // ==========================================
        // 工作表 3：3_充電槍資訊 (API-103)
        // ==========================================
        Sheet s3 = workbook.createSheet("3_充電槍資訊");
        s3.setDisplayGridlines(true);
        Row s3H = s3.createRow(0);
        s3H.setHeightInPoints(26);
        createHeaderCell(s3H, 0, "場站代碼 (stationId) *", colHeaderStyle);
        createHeaderCell(s3H, 1, "充電樁序號 (chargerId) *", colHeaderStyle);
        createHeaderCell(s3H, 2, "充電槍頭序號 (connectId) *", colHeaderStyle);
        createHeaderCell(s3H, 3, "首次啟用時間 (firstEnableDate) *", colHeaderStyle);

        // 示範一站多樁與一樁雙槍
        Row s3D1 = s3.createRow(1);
        s3D1.setHeightInPoints(22);
        createSampleCell(s3D1, 0, "[範例] ST_DEMO_01", sampleStyle);
        createSampleCell(s3D1, 1, "EVSE_01", sampleStyle);
        createSampleCell(s3D1, 2, "1", sampleStyle);
        createSampleCell(s3D1, 3, "2025-01-01", sampleStyle);

        Row s3D2 = s3.createRow(2);
        s3D2.setHeightInPoints(22);
        createSampleCell(s3D2, 0, "[範例] ST_DEMO_01", sampleStyle);
        createSampleCell(s3D2, 1, "EVSE_01", sampleStyle);
        createSampleCell(s3D2, 2, "2", sampleStyle);
        createSampleCell(s3D2, 3, "2025-01-01", sampleStyle);

        Row s3D3 = s3.createRow(3);
        s3D3.setHeightInPoints(22);
        createSampleCell(s3D3, 0, "[範例] ST_DEMO_01", sampleStyle);
        createSampleCell(s3D3, 1, "EVSE_02", sampleStyle);
        createSampleCell(s3D3, 2, "1", sampleStyle);
        createSampleCell(s3D3, 3, "2025-01-01", sampleStyle);

        s3.setColumnWidth(0, 7000);
        s3.setColumnWidth(1, 7000);
        s3.setColumnWidth(2, 7000);
        s3.setColumnWidth(3, 8500);

        // ==========================================
        // 工作表 4：4_充電交易明細 (API-104)
        // ==========================================
        Sheet s4 = workbook.createSheet("4_充電交易明細");
        s4.setDisplayGridlines(true);
        Row s4H = s4.createRow(0);
        s4H.setHeightInPoints(26);
        createHeaderCell(s4H, 0, "交易序號 (transNo) *", colHeaderStyle);
        createHeaderCell(s4H, 1, "場站代碼 (stationId) *", colHeaderStyle);
        createHeaderCell(s4H, 2, "充電樁代碼 (chargerId) *", colHeaderStyle);
        createHeaderCell(s4H, 3, "充電槍號 (connectId) *", colHeaderStyle);
        createHeaderCell(s4H, 4, "開始時間 (startTime) *", colHeaderStyle);
        createHeaderCell(s4H, 5, "結束時間 (endTime) *", colHeaderStyle);
        createHeaderCell(s4H, 6, "充電度數 (kwh) *", colHeaderStyle);

        Row s4D1 = s4.createRow(1);
        s4D1.setHeightInPoints(22);
        createSampleCell(s4D1, 0, "[範例] TR20261104001", sampleStyle);
        createSampleCell(s4D1, 1, "ST_DEMO_01", sampleStyle);
        createSampleCell(s4D1, 2, "EVSE_01", sampleStyle);
        createSampleCell(s4D1, 3, "1", sampleStyle);
        createSampleCell(s4D1, 4, "2026-11-04 14:00:00", sampleStyle);
        createSampleCell(s4D1, 5, "2026-11-04 14:40:00", sampleStyle);
        createSampleCell(s4D1, 6, "35.50", sampleStyle);

        Row s4D2 = s4.createRow(2);
        s4D2.setHeightInPoints(22);
        createSampleCell(s4D2, 0, "[範例] TR20261104002", sampleStyle);
        createSampleCell(s4D2, 1, "ST_DEMO_01", sampleStyle);
        createSampleCell(s4D2, 2, "EVSE_01", sampleStyle);
        createSampleCell(s4D2, 3, "2", sampleStyle);
        createSampleCell(s4D2, 4, "2026-11-04 14:10:00", sampleStyle);
        createSampleCell(s4D2, 5, "2026-11-04 14:50:00", sampleStyle);
        createSampleCell(s4D2, 6, "28.20", sampleStyle);

        s4.setColumnWidth(0, 8000);
        s4.setColumnWidth(1, 6500);
        s4.setColumnWidth(2, 6500);
        s4.setColumnWidth(3, 6000);
        s4.setColumnWidth(4, 7500);
        s4.setColumnWidth(5, 7500);
        s4.setColumnWidth(6, 6000);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out;
    }

    private void createHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createSampleCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 離線業者一站式解析入庫 (Vendor -> Station -> Charger -> Charge)
     */
    @Override
    @Transactional
    public ResultResponse importOfflineData(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return new ResultResponse("0001", "上傳之檔案為空，請選擇有效 Excel 檔案");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return new ResultResponse("0001", "檔案格式不正確，僅支援 Excel (.xlsx / .xls)");
        }

        DataFormatter df = new DataFormatter();
        String now = LocalDateTime.now().format(formatter);

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            // 尋找各工作表
            Sheet sVendor = findSheet(workbook, "1", "業者");
            Sheet sStation = findSheet(workbook, "2", "站");
            Sheet sCharger = findSheet(workbook, "3", "槍", "樁");
            Sheet sTrans = findSheet(workbook, "4", "交易");

            if (sVendor == null || sStation == null || sCharger == null || sTrans == null) {
                return new ResultResponse("0001", "Excel 缺少必要工作表，需包含：1_業者帳號、2_充電站資訊、3_充電槍資訊、4_充電交易明細");
            }

            // =========================================================
            // 1. 處理業者帳號 (API-101)
            // =========================================================
            String targetVendorId = null;
            String rawPassword = null;

            for (int r = 1; r <= sVendor.getLastRowNum(); r++) {
                Row row = sVendor.getRow(r);
                if (row == null) continue;
                String vId = cleanCell(df.formatCellValue(row.getCell(0)));
                String pwd = cleanCell(df.formatCellValue(row.getCell(1)));
                if (isSampleOrEmpty(vId)) continue;
                targetVendorId = vId;
                rawPassword = pwd;
                break; // 取第一筆非範例業者帳號
            }

            if (!StringUtils.hasText(targetVendorId) || !StringUtils.hasText(rawPassword)) {
                return new ResultResponse("0001", "Sheet 1【1_業者帳號】未填寫有效之業者代碼 (vendorId) 或密碼 (password)");
            }

            // 權限檢查：僅系統管理員 (ROLE_ADMIN) 具備離線建置與匯入權限，普通業者 (ROLE_USER) 不允許上傳/匯入
            if (!authorizationService.isCurrentUserAdmin()) {
                String currentLoggedInVendor = authorizationService.getCurrentVendorId();
                logger.warn("Non-admin user {} attempted to import offline data", currentLoggedInVendor);
                return new ResultResponse("0003", "權限不足，僅系統管理員 (ROLE_ADMIN) 可執行離線匯入功能");
            }

            boolean vendorCreated = false;
            CarbonVendor vendor = vendorRepository.findByVendorId(targetVendorId);
            if (vendor == null) {
                vendor = new CarbonVendor();
                vendor.setVendorId(targetVendorId);
                vendor.setPassword(passwordEncoder.encode(rawPassword));
                vendor.setStatus("1");
                vendor.setRole("ROLE_USER");
                vendor.setCreateTime(now);
                vendor.setUpdateTime(now);
                vendorRepository.save(vendor);
                vendorCreated = true;
                logger.info("Created new vendor account from offline import: {}", targetVendorId);
            } else {
                logger.info("Vendor account already exists: {}, will bind records to it", targetVendorId);
            }

            // =========================================================
            // 2. 處理充電站場域 (API-102)
            // =========================================================
            int stationsCreated = 0;
            int stationsExisting = 0;
            Map<String, CarbonStation> stationCache = new HashMap<>();

            for (int r = 1; r <= sStation.getLastRowNum(); r++) {
                Row row = sStation.getRow(r);
                if (row == null) continue;
                String stId = cleanCell(df.formatCellValue(row.getCell(0)));
                String stName = cleanCell(df.formatCellValue(row.getCell(1)));
                String lat = cleanCell(df.formatCellValue(row.getCell(2)));
                String lng = cleanCell(df.formatCellValue(row.getCell(3)));

                if (isSampleOrEmpty(stId)) continue;

                CarbonStation station = stationRepository.findByStationId(stId);
                if (station == null) {
                    station = new CarbonStation();
                    station.setStationId(stId);
                    station.setVendorId(targetVendorId);
                    station.setStationName(StringUtils.hasText(stName) ? stName : stId);
                    station.setGpsLatitude(lat);
                    station.setGpsLongitude(lng);
                    station.setStationStatus("1");
                    station.setCountry("TW");
                    station.setStorageEnable("0");
                    station.setPowerSource("1");
                    station.setCreateTime(now);
                    station.setUpdateTime(now);
                    stationRepository.save(station);
                    stationsCreated++;
                } else {
                    stationsExisting++;
                }
                stationCache.put(stId, station);
            }

            // =========================================================
            // 3. 處理充電槍資訊 (API-103)
            // =========================================================
            int chargersCreated = 0;
            int chargersExisting = 0;
            Map<String, CarbonCharger> chargerCache = new HashMap<>();

            for (int r = 1; r <= sCharger.getLastRowNum(); r++) {
                Row row = sCharger.getRow(r);
                if (row == null) continue;
                String stId = cleanCell(df.formatCellValue(row.getCell(0)));
                String chId = cleanCell(df.formatCellValue(row.getCell(1)));
                String conId = cleanCell(df.formatCellValue(row.getCell(2)));
                String enableDate = cleanCell(df.formatCellValue(row.getCell(3)));

                if (isSampleOrEmpty(stId) || isSampleOrEmpty(chId) || !StringUtils.hasText(conId)) continue;
                if (!StringUtils.hasText(stId)) {
                    // 若無填寫 stationId，嘗試綁定唯一存在的站
                    if (stationCache.size() == 1) {
                        stId = stationCache.keySet().iterator().next();
                    }
                }

                CarbonCharger charger = chargerRepository.findByChargerIdAndConnectId(chId, conId);
                if (charger == null) {
                    charger = new CarbonCharger();
                    charger.setChargerId(chId);
                    charger.setConnectId(conId);
                    charger.setStationId(stId);
                    charger.setVendorId(targetVendorId);
                    charger.setFirstEnableDate(StringUtils.hasText(enableDate) ? enableDate : now.substring(0, 10));
                    charger.setChargerStatus("1");
                    charger.setCreateTime(now);
                    charger.setUpdateTime(now);
                    chargerRepository.save(charger);
                    chargersCreated++;
                } else {
                    chargersExisting++;
                }
                chargerCache.put(chId + "_" + conId, charger);
            }

            // =========================================================
            // 4. 處理充電交易記錄 (API-104)
            // =========================================================
            int transTotal = 0;
            int transInserted = 0;
            int transSkipped0006 = 0;
            List<String> errorLines = new ArrayList<>();
            List<CarbonCharge> newCharges = new ArrayList<>();

            for (int r = 1; r <= sTrans.getLastRowNum(); r++) {
                Row row = sTrans.getRow(r);
                if (row == null) continue;
                String transNo = cleanCell(df.formatCellValue(row.getCell(0)));
                String stId = cleanCell(df.formatCellValue(row.getCell(1)));
                String chId = cleanCell(df.formatCellValue(row.getCell(2)));
                String conId = cleanCell(df.formatCellValue(row.getCell(3)));
                String startTime = cleanCell(df.formatCellValue(row.getCell(4)));
                String endTime = cleanCell(df.formatCellValue(row.getCell(5)));
                String kwh = cleanCell(df.formatCellValue(row.getCell(6)));

                if (isSampleOrEmpty(transNo) || isSampleOrEmpty(stId) || isSampleOrEmpty(chId)) continue;
                transTotal++;

                // 防重檢核 (0006)
                CarbonCharge existingCharge = chargeRepository.findByTransNo(transNo);
                if (existingCharge != null) {
                    transSkipped0006++;
                    continue;
                }

                // 檢核必填欄位
                if (!StringUtils.hasText(stId) || !StringUtils.hasText(chId) || !StringUtils.hasText(conId) ||
                    !StringUtils.hasText(startTime) || !StringUtils.hasText(endTime) || !StringUtils.hasText(kwh)) {
                    errorLines.add("第 " + (r + 1) + " 列：交易單號 [" + transNo + "] 必填欄位不完整");
                    continue;
                }

                // 檢核度數
                try {
                    double kwhVal = Double.parseDouble(kwh);
                    if (kwhVal <= 0) {
                        errorLines.add("第 " + (r + 1) + " 列：交易單號 [" + transNo + "] 度數小於等於 0");
                        continue;
                    }
                } catch (Exception e) {
                    errorLines.add("第 " + (r + 1) + " 列：交易單號 [" + transNo + "] 度數數值格式無效");
                    continue;
                }

                CarbonCharge charge = new CarbonCharge();
                charge.setTransNo(transNo);
                charge.setVendorId(targetVendorId);
                charge.setStationId(stId);
                charge.setChargerId(chId);
                charge.setConnectId(conId);
                charge.setStartTime(convertToUtcZero(startTime));
                charge.setEndTime(convertToUtcZero(endTime));
                charge.setKwh(kwh);
                charge.setCarbonCredit("1"); // 預設同意碳權
                charge.setCreateTime(now);
                charge.setUpdateTime(now);

                newCharges.add(charge);
                transInserted++;

                // 批量持久化 (每 500 筆寫入一次)
                if (newCharges.size() >= 500) {
                    chargeRepository.saveAll(newCharges);
                    newCharges.clear();
                }
            }

            if (!newCharges.isEmpty()) {
                chargeRepository.saveAll(newCharges);
            }

            // 組裝成果報告訊息
            StringBuilder summary = new StringBuilder();
            summary.append("一站式離線建置與匯入完成！\n");
            summary.append("• 業者帳號：").append(targetVendorId)
                   .append(vendorCreated ? " (★新建成功，可直接以此帳密登入)" : " (已存在)")
                   .append("\n");
            summary.append("• 充電站點：新建 ").append(stationsCreated).append(" 座，既有 ").append(stationsExisting).append(" 座\n");
            summary.append("• 充電槍頭：新建 ").append(chargersCreated).append(" 支，既有 ").append(chargersExisting).append(" 支\n");
            summary.append("• 充電交易：成功寫入 ").append(transInserted).append(" 筆，重複略過 (0006) ").append(transSkipped0006).append(" 筆");

            if (!errorLines.isEmpty()) {
                summary.append("\n• 異常略過：").append(errorLines.size()).append(" 筆（前3筆：")
                       .append(String.join("; ", errorLines.subList(0, Math.min(3, errorLines.size()))))
                       .append("）");
            }

            logger.info("Offline onboarding summary: {}", summary.toString());
            return new ResultResponse("0000", summary.toString());
        }
    }

    private Sheet findSheet(Workbook wb, String... keywords) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i);
            for (String kw : keywords) {
                if (name.contains(kw)) {
                    return wb.getSheetAt(i);
                }
            }
        }
        return null;
    }

    private String cleanCell(String str) {
        if (str == null) return "";
        return str.trim();
    }

    private boolean isSampleOrEmpty(String val) {
        if (!StringUtils.hasText(val)) return true;
        String lower = val.toLowerCase();
        return lower.contains("[範例]") || lower.contains("[example]") || lower.startsWith("demo_");
    }

    /**
     * 將台灣時間 (UTC+8) 正規化為 UTC+0 儲存
     */
    private String convertToUtcZero(String timeStr) {
        if (!StringUtils.hasText(timeStr)) return timeStr;
        String trimmed = timeStr.trim();
        try {
            if (trimmed.length() == 19) {
                LocalDateTime localDt = LocalDateTime.parse(trimmed, formatter);
                ZonedDateTime taipeiZoned = localDt.atZone(ZoneId.of("Asia/Taipei"));
                ZonedDateTime utcZoned = taipeiZoned.withZoneSameInstant(ZoneId.of("UTC"));
                return utcZoned.format(formatter);
            }
        } catch (Exception e) {
            logger.warn("Time parse failed for: {}, keep original", trimmed);
        }
        return trimmed;
    }
}
