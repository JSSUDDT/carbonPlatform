package com.carbon.api.controller;

import com.carbon.api.entity.CarbonCharger;
import com.carbon.api.entity.CarbonStation;
import com.carbon.api.service.ChargerService;
import com.carbon.api.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 處理前端頁面路由的控制器
 * 將請求映射到相應的 Thymeleaf 模板
 */
@Controller
public class ViewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    
    @Autowired
    private ChargerService chargerService;
    
    @Autowired
    private StationService stationService;
    
    @Autowired
    private MessageSource messageSource;

    /**
     * 提供i18n翻譯的API
     */
    @GetMapping("/api/i18n/messages")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getMessages(@RequestParam(defaultValue = "en") String locale) {
        Locale currentLocale = locale.equals("zh_TW") ? Locale.TRADITIONAL_CHINESE : Locale.ENGLISH;
        Map<String, String> translations = new HashMap<>();
        
        // 添加站點地圖相關的翻譯
        translations.put("stationmap.admin.view", messageSource.getMessage("stationmap.admin.view", null, currentLocale));
        translations.put("stationmap.user.view", messageSource.getMessage("stationmap.user.view", null, currentLocale));
        translations.put("stationmap.noStations", messageSource.getMessage("stationmap.noStations", null, currentLocale));
        
        return ResponseEntity.ok(translations);
    }

    /**
     * 登錄頁面
     */
    @GetMapping({"/", "/login", "/login.html"})
    public String login() {
        return "login";
    }
    
    /**
     * 註冊頁面
     */
    @GetMapping({"/register", "/register.html"})
    public String register() {
        return "register";
    }
    
    /**
     * 充電站管理頁面
     */
    @GetMapping({"/stations", "/stations.html"})
    public String stations() {
        return "stations";
    }
    
    /**
     * 充電樁管理頁面
     */
    @GetMapping({"/chargers", "/chargers.html"})
    public String chargers() {
        return "chargers";
    }
    
    /**
     * 充電記錄頁面
     */
    @GetMapping({"/transactions", "/transactions.html"})
    public String transactions() {
        return "transactions";
    }
    
    /**
     * 充電站電力頁面
     */
    @GetMapping({"/stationpower", "/stationpower.html"})
    public String stationPower() {
        return "stationpower";
    }
    
    /**
     * 充電站地圖頁面
     */
    @GetMapping({"/stationmap", "/stationmap.html"})
    public String stationMap() {
        // 通過前端JavaScript中的認證檢查實現訪問控制
        return "stationmap";
    }
    
    /**
     * 處理充電樁管理頁面請求（帶參數）
     * 支持搜索、站點過濾和狀態過濾
     */
    @GetMapping("/chargers/filter")
    public String chargersWithFilter(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        
        logger.debug("Processing chargers page request, search: {}, stationId: {}, status: {}, page: {}", 
                search, stationId, status, page);
        
        // 獲取充電樁列表（使用新的實現了搜索功能的方法）
        List<CarbonCharger> chargers = chargerService.getChargerInfo(null, stationId, search, status);
        
        // 獲取所有充電站列表（用於下拉選單）
        List<CarbonStation> stations = stationService.getStationInfo(null);
        
        // 設置頁面參數
        model.addAttribute("chargers", chargers);
        model.addAttribute("stations", stations);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", Math.max(1, (int) Math.ceil(chargers.size() / 10.0))); // 每頁10條記錄
        
        // 保留搜索參數，方便分頁
        model.addAttribute("search", search);
        model.addAttribute("selectedStationId", stationId);
        model.addAttribute("selectedStatus", status);
        
        return "chargers";
    }
} 