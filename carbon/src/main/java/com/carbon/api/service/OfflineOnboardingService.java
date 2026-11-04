package com.carbon.api.service;

import com.carbon.api.dto.ResultResponse;
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
     * 解析上傳的 Excel 檔案並執行階層式自動入庫
     * @param file 上傳的 .xlsx 檔案
     * @return 包含開通與入庫統計結果之 Response
     * @throws Exception 解析或處理異常
     */
    ResultResponse importOfflineData(MultipartFile file) throws Exception;
}
