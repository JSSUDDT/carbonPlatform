package com.carbon.api.service;

import com.carbon.api.dto.OfflineImportJob;

/**
 * 已有離線匯入任務執行中時拋出 (同一時間僅允許一個匯入任務)
 */
public class ImportBusyException extends RuntimeException {

    private final transient OfflineImportJob runningJob;

    public ImportBusyException(OfflineImportJob runningJob) {
        super("目前已有匯入任務執行中，請等待完成");
        this.runningJob = runningJob;
    }

    public OfflineImportJob getRunningJob() {
        return runningJob;
    }
}
