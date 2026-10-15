package com.carbon.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SyncResult {
    private boolean success;
    private String message;
    private int totalRecords;
    private int syncedRecords;
    private int skippedRecords;
    private int errorRecords;
    private LocalDateTime syncTime;
    private String errorDetails;
    
    // 靜態方法創建成功結果
    public static SyncResult success(int total, int synced, int skipped) {
        return SyncResult.builder()
                .success(true)
                .message("同步成功")
                .totalRecords(total)
                .syncedRecords(synced)
                .skippedRecords(skipped)
                .errorRecords(0)
                .syncTime(LocalDateTime.now())
                .build();
    }
    
    // 靜態方法創建失敗結果
    public static SyncResult failure(String message, String errorDetails) {
        return SyncResult.builder()
                .success(false)
                .message(message)
                .errorDetails(errorDetails)
                .syncTime(LocalDateTime.now())
                .build();
    }
}
