package com.carbon.api.config;

import com.carbon.api.dto.ResultResponse;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 處理JSON解析異常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.error("JSON解析失敗: {}", ex.getMessage());
        
        // 處理未識別的屬性異常 (如vendorID而非vendorId)
        if (ex.getCause() instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException cause = (UnrecognizedPropertyException) ex.getCause();
            String propertyName = cause.getPropertyName();
            String[] knownPropertyNames = cause.getKnownPropertyIds().toArray(new String[0]);
            
            // 檢查是否是大小寫錯誤的常見情況
            String suggestion = findSimilarProperty(propertyName, knownPropertyNames);
            
            if (suggestion != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResultResponse("0001", String.format(
                                "參數名稱錯誤: '%s' 不是有效的參數名稱，您是否想使用 '%s'？請使用正確的大小寫格式。", 
                                propertyName, suggestion)));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResultResponse("0001", String.format(
                                "參數名稱錯誤: '%s' 不是有效的參數名稱，請檢查API文檔。", propertyName)));
            }
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "請求參數格式錯誤，請檢查JSON格式是否正確"));
    }
    
    /**
     * 查找相似屬性名稱
     */
    private String findSimilarProperty(String propertyName, String[] knownProperties) {
        // 檢查常見的大小寫問題，如vendorID vs vendorId
        String propertyNameLower = propertyName.toLowerCase();
        
        for (String known : knownProperties) {
            if (known.toLowerCase().equals(propertyNameLower)) {
                return known;
            }
        }
        
        return null;
    }
    
    /**
     * 處理請求參數綁定異常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResultResponse> handleBindException(BindException ex) {
        logger.error("參數綁定失敗: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "請求參數綁定錯誤，請確保參數名稱和類型正確"));
    }
    
    /**
     * 處理請求參數驗證異常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        logger.error("參數驗證失敗: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "參數驗證錯誤: " + errorMessage));
    }
    
    /**
     * 處理請求參數類型錯誤
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResultResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        logger.error("參數類型不匹配: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "參數類型錯誤，請確保參數值符合預期類型"));
    }
    
    /**
     * 處理缺少必須參數錯誤
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResultResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        logger.error("缺少必須參數: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "缺少必須參數: " + ex.getParameterName()));
    }
    
    /**
     * 處理約束驗證錯誤
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.error("參數約束驗證失敗: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse("0001", "參數約束驗證錯誤: " + ex.getMessage()));
    }
    
    /**
     * 處理所有其他異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultResponse> handleException(Exception ex) {
        logger.error("系統異常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResultResponse("9999", "系統異常，請聯繫管理員"));
    }
} 