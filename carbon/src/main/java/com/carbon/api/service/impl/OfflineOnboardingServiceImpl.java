package com.carbon.api.service.impl;

import com.carbon.api.dto.OfflineImportJob;
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
import com.carbon.api.service.ImportBusyException;
import com.carbon.api.service.OfflineOnboardingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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

    // =====================================================================
    // 離線匯入：背景任務 + 分批提交 + 進度回報
    // =====================================================================

    private static final int CHUNK_SIZE = 300;
    private static final int MAX_JOBS_KEPT = 20;

    @Autowired
    private PlatformTransactionManager txManager;

    @PersistenceContext
    private EntityManager em;

    private final Map<String, OfflineImportJob> jobs = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicReference<OfflineImportJob> runningJob = new AtomicReference<>();
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "offline-import");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdownExecutor() {
        importExecutor.shutdownNow();
    }

    /** 匯入過程中的共用狀態 */
    private static class ImportCtx {
        String vendorId;
        boolean vendorCreated;
        int stationsCreated;
        int stationsExisting;
        int chargersCreated;
        int chargersExisting;
        final Map<String, CarbonStation> stationCache = new HashMap<>();
        final Map<String, CarbonCharger> chargerCache = new HashMap<>();
        /** 首次交易時間有變動、需在最後回寫的充電槍 */
        final Set<String> firstTxDirty = new HashSet<>();
    }

    /** 通過基本檢核、待查重的交易列 */
    private static class RawTrans {
        int rowNo; // Excel 實際列號 (從 1 起算)
        String transNo, stId, chId, conId, startTime, endTime, kwh;
    }

    /** 通過所有檢核、待寫入的交易 */
    private static class PendingCharge {
        final CarbonCharge charge;
        final String gunKey;

        PendingCharge(CarbonCharge charge, String gunKey) {
            this.charge = charge;
            this.gunKey = gunKey;
        }
    }

    @Override
    public OfflineImportJob startImport(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上傳之檔案為空，請選擇有效 Excel 檔案");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("檔案格式不正確，僅支援 Excel (.xlsx / .xls)");
        }

        OfflineImportJob job = new OfflineImportJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setFileName(fileName);

        // 同一時間只允許一個匯入任務，避免重複點擊或並行匯入造成資料衝突
        if (!runningJob.compareAndSet(null, job)) {
            OfflineImportJob current = runningJob.get();
            throw new ImportBusyException(current != null ? current : job);
        }

        try {
            // 請求結束後暫存檔會被清除，因此先讀入記憶體再交給背景執行緒
            final byte[] bytes = file.getBytes();
            jobs.put(job.getJobId(), job);
            trimJobs();
            importExecutor.submit(() -> runImport(job, bytes));
        } catch (Exception e) {
            runningJob.compareAndSet(job, null);
            jobs.remove(job.getJobId());
            throw e;
        }
        return job;
    }

    @Override
    public OfflineImportJob getJob(String jobId) {
        return jobId == null ? null : jobs.get(jobId);
    }

    @Override
    public OfflineImportJob getCurrentJob() {
        OfflineImportJob running = runningJob.get();
        if (running != null) {
            return running;
        }
        synchronized (jobs) {
            OfflineImportJob last = null;
            for (OfflineImportJob j : jobs.values()) {
                last = j;
            }
            return last;
        }
    }

    private void trimJobs() {
        synchronized (jobs) {
            while (jobs.size() > MAX_JOBS_KEPT) {
                Iterator<Map.Entry<String, OfflineImportJob>> it = jobs.entrySet().iterator();
                if (!it.hasNext()) break;
                Map.Entry<String, OfflineImportJob> eldest = it.next();
                if (eldest.getValue() == runningJob.get()) break;
                it.remove();
            }
        }
    }

    private void runImport(OfflineImportJob job, byte[] bytes) {
        long startNs = System.nanoTime();
        job.setStartTime(LocalDateTime.now().format(formatter));
        job.setStatus("RUNNING");
        try {
            ResultResponse result = doImport(job, bytes, startNs);
            job.setResultCode(result.getResultCode());
            job.setResultMsg(result.getResultMsg());
            job.setStatus("0000".equals(result.getResultCode()) ? "DONE" : "FAILED");
        } catch (Throwable e) {
            logger.error("離線業者一站式匯入處理異常", e);
            job.setResultCode("0001");
            job.setResultMsg("匯入失敗: " + e.getMessage());
            job.setStatus("FAILED");
        } finally {
            job.setPhase("已結束");
            job.setEndTime(LocalDateTime.now().format(formatter));
            runningJob.compareAndSet(job, null);
        }
    }

    /**
     * 離線業者一站式解析入庫 (Vendor -> Station -> Charger -> Charge)
     * 1. 業者/場站/充電槍：單一短交易提交
     * 2. 充電交易：每 CHUNK_SIZE 筆為一個交易，批次查重 + persist，失敗的批次退回逐筆處理
     * 3. 單筆資料錯誤只略過該筆，其餘正常資料照常匯入
     */
    private ResultResponse doImport(OfflineImportJob job, byte[] bytes, long startNs) throws Exception {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        String now = LocalDateTime.now().format(formatter);

        try (InputStream is = new ByteArrayInputStream(bytes); Workbook workbook = WorkbookFactory.create(is)) {
            job.setPhase("解析 Excel 工作表");
            Sheet sVendor = findSheet(workbook, "1", "業者");
            Sheet sStation = findSheet(workbook, "2", "站");
            Sheet sCharger = findSheet(workbook, "3", "槍", "樁");
            Sheet sTrans = findSheet(workbook, "4", "交易");

            if (sVendor == null || sStation == null || sCharger == null || sTrans == null) {
                return new ResultResponse("0001", "Excel 缺少必要工作表，需包含：1_業者帳號、2_充電站資訊、3_充電槍資訊、4_充電交易明細");
            }

            // ---------- 階段 1：業者 / 場站 / 充電槍 ----------
            job.setPhase("建立業者、場站與充電槍");
            ImportCtx ctx = new ImportCtx();
            ResultResponse abort = tt.execute(st -> setupMasterData(job, ctx, sVendor, sStation, sCharger, now));
            if (abort != null) {
                return abort;
            }

            // ---------- 階段 2：充電交易 ----------
            job.setPhase("寫入充電交易");
            DataFormatter df = new DataFormatter();
            Set<String> seenTransNoInExcel = new HashSet<>();
            int lastRow = sTrans.getLastRowNum();
            job.setTotalRows(Math.max(lastRow, 0));
            List<RawTrans> chunk = new ArrayList<>(CHUNK_SIZE);

            for (int r = 1; r <= lastRow; r++) {
                Row row = sTrans.getRow(r);
                if (row != null) {
                    RawTrans raw = new RawTrans();
                    raw.rowNo = r + 1;
                    raw.transNo = cleanCell(df.formatCellValue(row.getCell(0)));
                    raw.stId = cleanCell(df.formatCellValue(row.getCell(1)));
                    raw.chId = cleanCell(df.formatCellValue(row.getCell(2)));
                    raw.conId = cleanCell(df.formatCellValue(row.getCell(3)));
                    raw.startTime = cleanCell(df.formatCellValue(row.getCell(4)));
                    raw.endTime = cleanCell(df.formatCellValue(row.getCell(5)));
                    raw.kwh = cleanCell(df.formatCellValue(row.getCell(6)));

                    if (!(isSampleOrEmpty(raw.transNo) || isSampleOrEmpty(raw.stId) || isSampleOrEmpty(raw.chId))) {
                        if (!StringUtils.hasText(raw.transNo) || !StringUtils.hasText(raw.stId) || !StringUtils.hasText(raw.chId)
                                || !StringUtils.hasText(raw.conId) || !StringUtils.hasText(raw.startTime)
                                || !StringUtils.hasText(raw.endTime) || !StringUtils.hasText(raw.kwh)) {
                            job.addError("第 " + raw.rowNo + " 列 [單號 " + (StringUtils.hasText(raw.transNo) ? raw.transNo : "空") + "]：必填欄位不完整");
                        } else if (!seenTransNoInExcel.add(raw.transNo)) {
                            job.addError("第 " + raw.rowNo + " 列 [單號 " + raw.transNo + "]：Excel 表內交易單號重複");
                        } else {
                            chunk.add(raw);
                        }
                    }
                }
                if (chunk.size() >= CHUNK_SIZE) {
                    processChunk(job, ctx, tt, chunk, now);
                    chunk.clear();
                    job.setProcessedRows(r);
                }
            }
            if (!chunk.isEmpty()) {
                processChunk(job, ctx, tt, chunk, now);
                chunk.clear();
            }
            job.setProcessedRows(job.getTotalRows());

            // ---------- 階段 3：回寫充電槍首次交易時間 ----------
            job.setPhase("回寫充電槍首次交易時間");
            if (!ctx.firstTxDirty.isEmpty()) {
                try {
                    tt.execute(st -> {
                        for (String key : ctx.firstTxDirty) {
                            CarbonCharger ch = ctx.chargerCache.get(key);
                            if (ch != null) {
                                chargerRepository.save(ch);
                            }
                        }
                        return null;
                    });
                } catch (Exception e) {
                    logger.warn("Failed to auto-update charger firstTransactionTime: {}", e.getMessage());
                }
            }

            // ---------- 成果報告 ----------
            long seconds = (System.nanoTime() - startNs) / 1_000_000_000L;
            StringBuilder summary = new StringBuilder();
            summary.append("一站式離線建置與匯入完成！(耗時 ").append(seconds).append(" 秒)\n");
            summary.append("• 業者帳號：").append(ctx.vendorId)
                    .append(ctx.vendorCreated ? " (★新建成功，可直接以此帳密登入)" : " (已存在)")
                    .append("\n");
            summary.append("• 充電站點：新建 ").append(ctx.stationsCreated).append(" 座，既有 ").append(ctx.stationsExisting).append(" 座\n");
            summary.append("• 充電槍頭：新建 ").append(ctx.chargersCreated).append(" 支，既有 ").append(ctx.chargersExisting).append(" 支\n");
            summary.append("• 充電交易：成功寫入 ").append(job.getInserted()).append(" 筆，重複略過 (0006) ").append(job.getSkippedDuplicate()).append(" 筆");
            if (job.getErrorCount() > 0) {
                List<String> errs = job.getErrors();
                summary.append("\n• 異常略過：").append(job.getErrorCount()).append(" 筆（前5筆：")
                        .append(String.join("; ", errs.subList(0, Math.min(5, errs.size()))))
                        .append("）");
            }
            logger.info("Offline onboarding summary: {}", summary);
            return new ResultResponse("0000", summary.toString());
        }
    }

    /**
     * 階段 1：建立/比對業者、場站、充電槍。
     * 業者層級的錯誤(代碼/密碼格式)會中止匯入；場站與充電槍的單列錯誤只略過該列，
     * 其交易會因「對應充電設備不存在」被列入異常。
     * @return null 表示成功；非 null 表示中止並回傳訊息
     */
    private ResultResponse setupMasterData(OfflineImportJob job, ImportCtx ctx,
                                           Sheet sVendor, Sheet sStation, Sheet sCharger, String now) {
        DataFormatter df = new DataFormatter();

        // ---- 1. 業者帳號 (API-101) ----
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

        if (!StringUtils.hasText(targetVendorId)) {
            return new ResultResponse("0001", "Sheet 1【1_業者帳號】未填寫有效之業者代碼 (vendorId)");
        }
        if (!targetVendorId.matches("^[a-zA-Z0-9._@\\-]{3,50}$")) {
            return new ResultResponse("0001", "Sheet 1【1_業者帳號】業者代碼 [" + targetVendorId + "] 格式無效（限3~50字元之英數字、減號、底線、小數點或Email格式，且不可包含空格或特殊符號）");
        }
        ctx.vendorId = targetVendorId;

        CarbonVendor vendor = vendorRepository.findByVendorId(targetVendorId);
        if (vendor == null) {
            if (!StringUtils.hasText(rawPassword) || isSampleOrEmpty(rawPassword) || rawPassword.length() < 6) {
                return new ResultResponse("0001", "Sheet 1【1_業者帳號】新業者預設密碼長度至少需6位數，且不可使用範例密碼");
            }
            vendor = new CarbonVendor();
            vendor.setVendorId(targetVendorId);
            vendor.setPassword(passwordEncoder.encode(rawPassword));
            vendor.setStatus("1");
            vendor.setRole("ROLE_USER");
            vendor.setCreateTime(now);
            vendor.setUpdateTime(now);
            vendorRepository.save(vendor);
            ctx.vendorCreated = true;
            logger.info("Created new vendor account from offline import: {}", targetVendorId);
        } else {
            logger.info("Vendor account already exists: {}, will keep existing password and bind records to it", targetVendorId);
        }

        // ---- 2. 充電站場域 (API-102) ----
        Set<String> seenStationIdsInExcel = new HashSet<>();
        for (int r = 1; r <= sStation.getLastRowNum(); r++) {
            Row row = sStation.getRow(r);
            if (row == null) continue;
            String stId = cleanCell(df.formatCellValue(row.getCell(0)));
            String stName = cleanCell(df.formatCellValue(row.getCell(1)));
            String lat = cleanCell(df.formatCellValue(row.getCell(2)));
            String lng = cleanCell(df.formatCellValue(row.getCell(3)));

            if (isSampleOrEmpty(stId)) continue;
            String loc = "Sheet 2【2_充電站資訊】第 " + (r + 1) + " 列：";

            if (!seenStationIdsInExcel.add(stId)) {
                job.addError(loc + "場站代碼 [" + stId + "] 在工作表中重複定義");
                continue;
            }
            if (stId.length() > 50) {
                job.addError(loc + "場站代碼 [" + stId + "] 長度超出50字元上限");
                continue;
            }
            if (!StringUtils.hasText(lat) || !StringUtils.hasText(lng)) {
                job.addError(loc + "場站 [" + stId + "] GPS緯度與經度為必填欄位");
                continue;
            }
            double latVal, lngVal;
            try {
                latVal = Double.parseDouble(lat);
            } catch (Exception e) {
                job.addError(loc + "場站 [" + stId + "] GPS緯度非合法數值格式: " + lat);
                continue;
            }
            if (latVal < -90.0 || latVal > 90.0) {
                job.addError(loc + "場站 [" + stId + "] GPS緯度超出合理範圍 (-90 ~ +90): " + latVal);
                continue;
            }
            try {
                lngVal = Double.parseDouble(lng);
            } catch (Exception e) {
                job.addError(loc + "場站 [" + stId + "] GPS經度非合法數值格式: " + lng);
                continue;
            }
            if (lngVal < -180.0 || lngVal > 180.0) {
                job.addError(loc + "場站 [" + stId + "] GPS經度超出合理範圍 (-180 ~ +180): " + lngVal);
                continue;
            }

            CarbonStation station = stationRepository.findByVendorIdAndStationId(targetVendorId, stId);
            if (station == null) {
                station = new CarbonStation();
                station.setVendorId(targetVendorId);
                station.setStationId(stId);
                station.setStationName(StringUtils.hasText(stName) ? stName : stId);
                station.setGpsLatitude(String.valueOf(latVal));
                station.setGpsLongitude(String.valueOf(lngVal));
                station.setStationStatus("1");
                station.setCountry("TW");
                station.setStorageEnable("0");
                station.setPowerSource("1");
                station.setCreateTime(now);
                station.setUpdateTime(now);
                stationRepository.save(station);
                ctx.stationsCreated++;
            } else {
                ctx.stationsExisting++;
            }
            ctx.stationCache.put(stId, station);
        }

        // ---- 3. 充電槍資訊 (API-103) ----
        Set<String> seenChargerGunsInExcel = new HashSet<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Taipei"));
        LocalDate minEnableDate = LocalDate.of(2010, 1, 1);

        for (int r = 1; r <= sCharger.getLastRowNum(); r++) {
            Row row = sCharger.getRow(r);
            if (row == null) continue;
            String stId = cleanCell(df.formatCellValue(row.getCell(0)));
            String chId = cleanCell(df.formatCellValue(row.getCell(1)));
            String conId = cleanCell(df.formatCellValue(row.getCell(2)));
            String enableDate = cleanCell(df.formatCellValue(row.getCell(3)));

            if (isSampleOrEmpty(stId) || isSampleOrEmpty(chId)) continue;
            String loc = "Sheet 3【3_充電槍資訊】第 " + (r + 1) + " 列：";

            if (!StringUtils.hasText(stId)) {
                if (ctx.stationCache.size() == 1) {
                    stId = ctx.stationCache.keySet().iterator().next();
                } else {
                    job.addError(loc + "未填寫所屬場站代碼 (stationId)");
                    continue;
                }
            }
            if (!ctx.stationCache.containsKey(stId)) {
                CarbonStation dbStation = stationRepository.findByVendorIdAndStationId(targetVendorId, stId);
                if (dbStation == null) {
                    job.addError(loc + "指定之場站代碼 [" + stId + "] 不存在於 Sheet 2 亦未在資料庫中");
                    continue;
                }
                ctx.stationCache.put(stId, dbStation);
            }
            if (!StringUtils.hasText(chId) || chId.length() > 50) {
                job.addError(loc + "充電樁序號 (chargerId) 不得為空且長度限 50 字元以內");
                continue;
            }
            if (!StringUtils.hasText(conId) || !conId.matches("^[1-9]\\d{0,3}$")) {
                job.addError(loc + "充電槍序號 (connectId) [" + conId + "] 格式無效，必須為正整數 (如 1, 2)");
                continue;
            }
            String chargerGunKey = stId + "#" + chId + "#" + conId;
            if (!seenChargerGunsInExcel.add(chargerGunKey)) {
                job.addError(loc + "充電槍 [" + chId + " - 槍 " + conId + "] 於同場站重複定義");
                continue;
            }
            if (!StringUtils.hasText(enableDate)) {
                job.addError(loc + "首次啟用時間 (firstEnableDate) 不可留空");
                continue;
            }
            LocalDate parsedEnableDate;
            try {
                parsedEnableDate = LocalDate.parse(enableDate);
            } catch (Exception e) {
                job.addError(loc + "首次啟用時間 [" + enableDate + "] 格式無效，需為 YYYY-MM-DD (例如 2025-01-01)");
                continue;
            }
            if (parsedEnableDate.isBefore(minEnableDate) || parsedEnableDate.isAfter(today.plusDays(1))) {
                job.addError(loc + "首次啟用時間 [" + enableDate + "] 超出合理範圍（需介於 2010 年至今日）");
                continue;
            }

            CarbonCharger charger = chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(targetVendorId, stId, chId, conId);
            if (charger == null) {
                charger = new CarbonCharger();
                charger.setVendorId(targetVendorId);
                charger.setStationId(stId);
                charger.setChargerId(chId);
                charger.setConnectId(conId);
                charger.setFirstEnableDate(enableDate);
                charger.setChargerStatus("1");
                charger.setCreateTime(now);
                charger.setUpdateTime(now);
                chargerRepository.save(charger);
                ctx.chargersCreated++;
            } else {
                ctx.chargersExisting++;
            }
            ctx.chargerCache.put(chargerGunKey, charger);
        }
        return null;
    }

    /**
     * 處理一批交易：一次查出已存在的單號 (0006 防重) → 逐筆檢核 → 整批寫入。
     */
    private void processChunk(OfflineImportJob job, ImportCtx ctx, TransactionTemplate tt,
                              List<RawTrans> raws, String now) {
        LocalDateTime nowTaipei = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        List<String> ids = new ArrayList<>(raws.size());
        for (RawTrans raw : raws) {
            ids.add(raw.transNo);
        }
        Set<String> existing = new HashSet<>(chargeRepository.findExistingTransNos(ids));

        List<PendingCharge> pending = new ArrayList<>(raws.size());
        for (RawTrans raw : raws) {
            // 資料庫防重檢核 (代碼 0006)
            if (existing.contains(raw.transNo)) {
                job.setSkippedDuplicate(job.getSkippedDuplicate() + 1);
                continue;
            }
            PendingCharge pc = validateTrans(job, ctx, raw, nowTaipei, now);
            if (pc != null) {
                pending.add(pc);
            }
        }
        if (pending.isEmpty()) {
            return;
        }

        try {
            tt.execute(st -> {
                for (PendingCharge pc : pending) {
                    em.persist(pc.charge);
                }
                em.flush();
                em.clear();
                return null;
            });
            for (PendingCharge pc : pending) {
                trackFirstTransactionTime(ctx, pc.gunKey, pc.charge.getStartTime());
            }
            job.setInserted(job.getInserted() + pending.size());
        } catch (Exception batchEx) {
            // 整批失敗(例如與其他來源同時寫入造成主鍵衝突)：退回逐筆處理，只讓有問題的那筆失敗
            logger.warn("交易批次寫入失敗，改為逐筆處理: {}", batchEx.getMessage());
            for (PendingCharge pc : pending) {
                try {
                    Boolean inserted = tt.execute(st -> {
                        if (em.find(CarbonCharge.class, pc.charge.getTransNo()) != null) {
                            return Boolean.FALSE;
                        }
                        em.persist(pc.charge);
                        em.flush();
                        em.clear();
                        return Boolean.TRUE;
                    });
                    if (Boolean.TRUE.equals(inserted)) {
                        trackFirstTransactionTime(ctx, pc.gunKey, pc.charge.getStartTime());
                        job.setInserted(job.getInserted() + 1);
                    } else {
                        job.setSkippedDuplicate(job.getSkippedDuplicate() + 1);
                    }
                } catch (Exception rowEx) {
                    job.addError("[單號 " + pc.charge.getTransNo() + "]：寫入失敗: " + rowEx.getMessage());
                }
            }
        }
    }

    /** 單筆交易檢核；通過回傳待寫入物件，不通過記錄異常並回傳 null */
    private PendingCharge validateTrans(OfflineImportJob job, ImportCtx ctx, RawTrans raw,
                                        LocalDateTime nowTaipei, String now) {
        String head = "第 " + raw.rowNo + " 列 [單號 " + raw.transNo + "]：";

        // 設備存在性檢核（防幽靈槍/幽靈站交易）
        String gunKey = raw.stId + "#" + raw.chId + "#" + raw.conId;
        CarbonCharger targetCharger = ctx.chargerCache.get(gunKey);
        if (targetCharger == null) {
            targetCharger = chargerRepository.findByVendorIdAndStationIdAndChargerIdAndConnectId(ctx.vendorId, raw.stId, raw.chId, raw.conId);
            if (targetCharger == null) {
                job.addError(head + "對應充電設備不存在 (" + raw.stId + " / " + raw.chId + " / 槍 " + raw.conId + ")");
                return null;
            }
            ctx.chargerCache.put(gunKey, targetCharger);
        }

        LocalDateTime sDt, eDt;
        try {
            sDt = LocalDateTime.parse(raw.startTime, formatter);
            eDt = LocalDateTime.parse(raw.endTime, formatter);
        } catch (Exception e) {
            job.addError(head + "時間格式錯誤 (需為 YYYY-MM-DD HH:mm:ss)");
            return null;
        }
        if (!sDt.isBefore(eDt)) {
            job.addError(head + "開始時間 (" + raw.startTime + ") 必須早於結束時間 (" + raw.endTime + ")");
            return null;
        }
        if (sDt.isAfter(nowTaipei.plusHours(1))) {
            job.addError(head + "開始時間不可為未來時間 (" + raw.startTime + ")");
            return null;
        }
        long durationMinutes = Duration.between(sDt, eDt).toMinutes();
        if (durationMinutes > 72 * 60) {
            job.addError(head + "充電時長 (" + (durationMinutes / 60) + "小時) 超出合理極限 72 小時");
            return null;
        }
        String enableDateStr = targetCharger.getFirstEnableDate();
        if (StringUtils.hasText(enableDateStr)) {
            try {
                LocalDate enableDate = LocalDate.parse(enableDateStr);
                if (sDt.toLocalDate().isBefore(enableDate)) {
                    job.addError(head + "交易日期 (" + sDt.toLocalDate() + ") 早於設備首次啟用日期 (" + enableDateStr + ")");
                    return null;
                }
            } catch (Exception ignored) {
            }
        }
        double kwhVal;
        try {
            kwhVal = Double.parseDouble(raw.kwh);
        } catch (Exception e) {
            job.addError(head + "充電度數數值格式無效: " + raw.kwh);
            return null;
        }
        if (kwhVal <= 0.0) {
            job.addError(head + "充電度數必須大於 0 (" + raw.kwh + ")");
            return null;
        }
        if (kwhVal > 1000.0) {
            job.addError(head + "充電度數超出合理上限 1000 kWh (" + raw.kwh + ")");
            return null;
        }

        CarbonCharge charge = new CarbonCharge();
        charge.setTransNo(raw.transNo);
        charge.setVendorId(ctx.vendorId);
        charge.setStationId(raw.stId);
        charge.setChargerId(raw.chId);
        charge.setConnectId(raw.conId);
        charge.setStartTime(convertToUtcZero(raw.startTime));
        charge.setEndTime(convertToUtcZero(raw.endTime));
        charge.setKwh(String.format(Locale.US, "%.2f", kwhVal));
        charge.setCarbonCredit("1"); // 預設同意碳權
        charge.setCreateTime(now);
        charge.setUpdateTime(now);
        return new PendingCharge(charge, gunKey);
    }

    /** 記錄充電槍最早交易時間 (僅在記憶體更新，匯入結束後一次回寫) */
    private void trackFirstTransactionTime(ImportCtx ctx, String gunKey, String startTimeUtc) {
        CarbonCharger ch = ctx.chargerCache.get(gunKey);
        if (ch == null || startTimeUtc == null) return;
        String cur = ch.getFirstTransactionTime();
        if (!StringUtils.hasText(cur) || startTimeUtc.compareTo(cur) < 0) {
            ch.setFirstTransactionTime(startTimeUtc);
            ctx.firstTxDirty.add(gunKey);
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