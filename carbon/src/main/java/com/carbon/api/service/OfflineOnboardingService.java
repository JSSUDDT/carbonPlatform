package com.carbon.api.service;

import com.carbon.api.dto.OfflineImportJob;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

/**
 * 離線業者一站式建置與交易填報服務
 * 支援從零自動依序建置：業者帳號(API-101) -> 場站(API-102) -> 充電槍(API-103) -> 交易入庫(API-104)
 */
public interface OfflineOnboardingService {

    /**
     * 生成包含完整填寫指南、防呆指引與結構範例的標準 Excel (.xlsx) 範本
     * @return 範本檔案位元組串流
     * @throws Exception 產製異常
     */
    ByteArrayOutputStream generateTemplateExcel() throws Exception;

    /**
     * 受理離線匯入並於背景執行 (立即回傳任務，進度請以 getJob 輪詢)
     * @throws IllegalArgumentException 檔案為空或格式不正確
     * @throws ImportBusyException 已有匯入任務執行中
     */
    OfflineImportJob startImport(MultipartFile file) throws Exception;

    /** 依任務編號取得匯入進度 */
    OfflineImportJob getJob(String jobId);

    /** 取得執行中的任務；若無則回傳最近一次任務 (可能為 null) */
    OfflineImportJob getCurrentJob();
}