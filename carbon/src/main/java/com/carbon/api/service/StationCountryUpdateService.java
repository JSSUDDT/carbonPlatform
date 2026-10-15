package com.carbon.api.service;

/**
 * 充電站國家資訊批次更新服務接口
 */
public interface StationCountryUpdateService {
    
    /**
     * 批次更新所有充電站的國家資訊
     * 只更新沒有國家代碼或國家代碼為 "NA" 的充電站
     * 
     * @param forceUpdate 是否強制更新所有充電站（包括已有國家代碼的）
     * @param limit 限制處理的站點數量，0 或負數表示處理所有站點
     * @return 更新結果摘要
     */
    UpdateResult updateAllStationsCountry(boolean forceUpdate, int limit);
    
    /**
     * 更新結果摘要
     */
    class UpdateResult {
        private int totalStations;      // 總充電站數
        private int processedStations;  // 已處理的充電站數
        private int updatedStations;    // 成功更新的充電站數
        private int skippedStations;    // 跳過的充電站數（已有國家代碼且不強制更新）
        private int failedStations;     // 更新失敗的充電站數
        private String message;         // 結果訊息
        
        public UpdateResult() {
        }
        
        public UpdateResult(int totalStations, int processedStations, int updatedStations, 
                          int skippedStations, int failedStations, String message) {
            this.totalStations = totalStations;
            this.processedStations = processedStations;
            this.updatedStations = updatedStations;
            this.skippedStations = skippedStations;
            this.failedStations = failedStations;
            this.message = message;
        }
        
        // Getters and Setters
        public int getTotalStations() {
            return totalStations;
        }
        
        public void setTotalStations(int totalStations) {
            this.totalStations = totalStations;
        }
        
        public int getProcessedStations() {
            return processedStations;
        }
        
        public void setProcessedStations(int processedStations) {
            this.processedStations = processedStations;
        }
        
        public int getUpdatedStations() {
            return updatedStations;
        }
        
        public void setUpdatedStations(int updatedStations) {
            this.updatedStations = updatedStations;
        }
        
        public int getSkippedStations() {
            return skippedStations;
        }
        
        public void setSkippedStations(int skippedStations) {
            this.skippedStations = skippedStations;
        }
        
        public int getFailedStations() {
            return failedStations;
        }
        
        public void setFailedStations(int failedStations) {
            this.failedStations = failedStations;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}

