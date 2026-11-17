package com.carbon.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 離線業者匯入任務狀態 (背景執行，供前端輪詢進度)
 * status: QUEUED / RUNNING / DONE / FAILED
 */
@Data
public class OfflineImportJob {

    public static final int MAX_ERRORS_KEPT = 1000;

    private String jobId;
    private String fileName;
    private volatile String status = "QUEUED";
    private volatile String phase = "等待執行";

    /** 交易明細總列數 (含空列，作為進度分母) */
    private volatile int totalRows;
    private volatile int processedRows;
    private volatile int inserted;
    private volatile int skippedDuplicate;
    private volatile int errorCount;

    /** 最多保留前 MAX_ERRORS_KEPT 筆異常說明 */
    private final List<String> errors = new CopyOnWriteArrayList<>();

    private volatile String resultCode;
    private volatile String resultMsg;
    private volatile String startTime;
    private volatile String endTime;

    public void addError(String msg) {
        errorCount++;
        if (errors.size() < MAX_ERRORS_KEPT) {
            errors.add(msg);
        }
    }

    @JsonProperty("percent")
    public int getPercent() {
        if ("DONE".equals(status) || "FAILED".equals(status)) {
            return 100;
        }
        if (totalRows <= 0) {
            return 0;
        }
        return Math.min(99, (int) (processedRows * 100L / totalRows));
    }

    @JsonProperty("finished")
    public boolean isFinished() {
        return "DONE".equals(status) || "FAILED".equals(status);
    }
}
