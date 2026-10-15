# 碳權交易平台 - 開發規格書

## 📋 目錄
1. [專案概述](#專案概述)
2. [系統架構](#系統架構)
3. [技術規格](#技術規格)
4. [功能需求](#功能需求)
5. [API設計規格](#api設計規格)
6. [數據庫設計](#數據庫設計)
7. [安全規格](#安全規格)
8. [前端規格](#前端規格)
9. [部署規格](#部署規格)
10. [測試規格](#測試規格)
11. [維護規格](#維護規格)

---

## 🎯 專案概述

### 專案目標
開發一個完整的碳權交易平台，用於管理電動車充電設施的碳權交易業務，支援充電站、充電樁、充電記錄的管理，並提供碳權標記和交易功能。

### 業務價值
- 促進綠色能源發展
- 支援碳權交易市場
- 提供充電設施管理解決方案
- 實現碳排放減少的量化管理

### 目標用戶
- 充電設施運營商
- 電動車充電站管理員
- 碳權交易參與者
- 系統管理員

---

## 🏗️ 系統架構

### 整體架構
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端層        │    │   後端層        │    │   數據層        │
│                 │    │                 │    │                 │
│ • Thymeleaf     │◄──►│ • Spring Boot   │◄──►│ • MySQL         │
│ • Bootstrap     │    │ • Spring Security│    │ • Azure Cloud   │
│ • JavaScript    │    │ • JPA/Hibernate │    │ • 雲端數據庫     │
│ • 響應式設計    │    │ • JWT認證       │    │ • 自動備份      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 技術架構
- **前端**: Thymeleaf + Bootstrap 4 + JavaScript
- **後端**: Spring Boot 2.6.15 + Spring Security + JPA
- **數據庫**: MySQL 8.0 (Azure)
- **認證**: JWT Token
- **國際化**: 中英文雙語支援

---

## ⚙️ 技術規格

### 開發環境要求
- **Java**: JDK 8 或更高版本
- **Maven**: 3.6+ 
- **IDE**: IntelliJ IDEA 或 Eclipse
- **數據庫**: MySQL 8.0
- **瀏覽器**: Chrome, Firefox, Safari, Edge

### 運行環境要求
- **操作系統**: Linux/Windows Server
- **Java Runtime**: JRE 8+
- **記憶體**: 最少 2GB RAM
- **存儲空間**: 最少 10GB
- **網絡**: 穩定的互聯網連接

### 依賴庫規格
```xml
<!-- 核心依賴 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
```

---

## 📋 功能需求

### 1. 用戶管理模組
#### 1.1 用戶註冊
- **功能描述**: 新用戶註冊功能
- **輸入參數**: vendorId, password, confirmPassword, role
- **驗證規則**: 
  - vendorId: 必填，唯一性檢查
  - password: 最少8位，包含字母和數字
  - role: 預設為 ROLE_USER
- **輸出結果**: 註冊成功/失敗訊息

#### 1.2 用戶登錄
- **功能描述**: 用戶登錄認證
- **輸入參數**: vendorId, password
- **認證方式**: JWT Token
- **Token有效期**: 12小時
- **輸出結果**: JWT Token + 用戶信息

#### 1.3 權限管理
- **角色定義**:
  - ROLE_ADMIN: 系統管理員，可訪問所有功能
  - ROLE_USER: 普通用戶，只能訪問自己的數據
- **權限控制**: 基於Spring Security的RBAC

### 2. 充電站管理模組
#### 2.1 充電站信息管理
- **功能描述**: 充電站的CRUD操作
- **數據字段**:
  - stationId: 充電站ID (主鍵)
  - vendorId: 廠商ID
  - stationName: 充電站名稱
  - stationKwh: 充電站功率
  - city: 城市
  - address: 地址
  - gpsLatitude: GPS緯度
  - gpsLongitude: GPS經度
  - stationStatus: 狀態 (啟用/停用)

#### 2.2 充電站地圖展示
- **功能描述**: 在地圖上顯示充電站位置
- **地圖API**: 支援多種地圖服務
- **互動功能**: 點擊查看詳細信息

### 3. 充電樁管理模組
#### 3.1 充電樁信息管理
- **功能描述**: 充電樁的CRUD操作
- **複合主鍵**: chargerId + connectId
- **數據字段**:
  - chargerId: 充電樁ID
  - connectId: 連接ID
  - stationId: 所屬充電站
  - chargerType: 充電樁類型
  - chargerKw: 充電功率
  - chargerStatus: 狀態

#### 3.2 充電樁狀態監控
- **功能描述**: 實時監控充電樁狀態
- **狀態類型**: 可用、充電中、故障、維護

### 4. 充電記錄模組
#### 4.1 充電交易記錄
- **功能描述**: 記錄充電交易信息
- **數據字段**:
  - transNo: 交易編號 (主鍵)
  - vendorId: 廠商ID
  - stationId: 充電站ID
  - chargerId: 充電樁ID
  - startTime: 開始時間
  - endTime: 結束時間
  - kwh: 充電量
  - carbonCredit: 碳權標記

#### 4.2 碳權交易管理
- **功能描述**: 碳權標記和交易記錄
- **碳權標記**: Y/N 標識是否產生碳權
- **交易統計**: 按時間、地區、廠商統計

### 5. 電力監控模組
#### 5.1 充電站電力負載
- **功能描述**: 監控充電站電力使用情況
- **數據字段**:
  - stationId: 充電站ID
  - stationKwh: 充電站功率
  - powerLoad: 當前負載
  - timestamp: 時間戳

---

## 🔌 API設計規格

### API基礎規範
- **基礎URL**: `http://localhost:9095/carbon/api/vendor`
- **認證方式**: JWT Bearer Token
- **Content-Type**: `application/json`
- **字符編碼**: UTF-8

### 1. 認證相關API

#### 1.1 取得Token
```http
POST /api/vendor/GetToken
Content-Type: application/json

{
  "vendorId": "string",
  "password": "string"
}
```

**響應格式**:
```json
{
  "success": true,
  "message": "Token generated successfully",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 43200,
  "vendorId": "VENDOR001"
}
```

#### 1.2 用戶註冊
```http
POST /api/vendor/Register
Content-Type: application/json

{
  "vendorId": "string",
  "password": "string",
  "confirmPassword": "string",
  "role": "ROLE_USER"
}
```

### 2. 充電站管理API

#### 2.1 獲取充電站信息
```http
GET /api/vendor/StationInfo?vendorId={vendorId}&search={search}&status={status}
Authorization: Bearer {token}
```

#### 2.2 新增/更新充電站
```http
POST /api/vendor/StationInfo
Authorization: Bearer {token}
Content-Type: application/json

{
  "vendorId": "string",
  "stationAry": [
    {
      "stationId": "string",
      "stationName": "string",
      "stationKwh": "string",
      "city": "string",
      "address": "string",
      "gpsLatitude": "string",
      "gpsLongitude": "string",
      "stationStatus": "string"
    }
  ]
}
```

### 3. 充電樁管理API

#### 3.1 獲取充電樁信息
```http
GET /api/vendor/ChargerInfo?vendorId={vendorId}&stationId={stationId}&search={search}&status={status}
Authorization: Bearer {token}
```

#### 3.2 新增/更新充電樁
```http
POST /api/vendor/ChargerInfo
Authorization: Bearer {token}
Content-Type: application/json

{
  "vendorId": "string",
  "chargerAry": [
    {
      "chargerId": "string",
      "connectId": "string",
      "stationId": "string",
      "chargerType": "string",
      "chargerKw": "string",
      "chargerStatus": "string"
    }
  ]
}
```

### 4. 充電記錄API

#### 4.1 獲取充電記錄
```http
GET /api/vendor/ChargeInfo?vendorId={vendorId}&stationId={stationId}&chargerId={chargerId}&startTimeFrom={startTimeFrom}&startTimeTo={startTimeTo}
Authorization: Bearer {token}
```

#### 4.2 上傳充電記錄
```http
POST /api/vendor/ChargeInfo
Authorization: Bearer {token}
Content-Type: application/json

{
  "vendorId": "string",
  "chargeAry": [
    {
      "transNo": "string",
      "stationId": "string",
      "chargerId": "string",
      "connectId": "string",
      "startTime": "string",
      "endTime": "string",
      "kwh": "string",
      "carbonCredit": "string"
    }
  ]
}
```

### 5. 電力監控API

#### 5.1 獲取電力負載信息
```http
GET /api/vendor/StationPower?vendorId={vendorId}&stationId={stationId}
Authorization: Bearer {token}
```

#### 5.2 上傳電力負載信息
```http
POST /api/vendor/StationPower
Authorization: Bearer {token}
Content-Type: application/json

{
  "vendorId": "string",
  "powerAry": [
    {
      "stationId": "string",
      "stationKwh": "string",
      "powerLoad": "string"
    }
  ]
}
```

---

## 🗄️ 數據庫設計

### 數據庫配置
- **數據庫類型**: MySQL 8.0
- **字符編碼**: UTF-8
- **時區設置**: Asia/Taipei
- **連接池**: HikariCP

### 數據表設計

#### 1. carbon_vendor (廠商表)
```sql
CREATE TABLE carbon_vendor (
    vendor_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    status VARCHAR(1),
    role VARCHAR(20) DEFAULT 'ROLE_USER',
    create_time VARCHAR(20),
    update_time VARCHAR(20)
);
```

#### 2. carbon_station (充電站表)
```sql
CREATE TABLE carbon_station (
    station_id VARCHAR(50) PRIMARY KEY,
    vendor_id VARCHAR(50) NOT NULL,
    station_name VARCHAR(100),
    station_kwh VARCHAR(50),
    city VARCHAR(50),
    address VARCHAR(200),
    storage_enable VARCHAR(1),
    power_source VARCHAR(1),
    gps_latitude VARCHAR(50),
    gps_longitude VARCHAR(50),
    station_status VARCHAR(1),
    create_time VARCHAR(20),
    update_time VARCHAR(20)
);
```

#### 3. carbon_charger (充電樁表)
```sql
CREATE TABLE carbon_charger (
    charger_id VARCHAR(50),
    connect_id VARCHAR(50),
    station_id VARCHAR(50) NOT NULL,
    vendor_id VARCHAR(50) NOT NULL,
    charger_type VARCHAR(10),
    charger_brand VARCHAR(50),
    charger_kw VARCHAR(50),
    charger_a VARCHAR(50),
    charger_v VARCHAR(50),
    charger_status VARCHAR(1),
    create_time VARCHAR(20),
    update_time VARCHAR(20),
    first_enable_date VARCHAR(10),
    PRIMARY KEY (charger_id, connect_id)
);
```

#### 4. carbon_charge (充電記錄表)
```sql
CREATE TABLE carbon_charge (
    trans_no VARCHAR(50) PRIMARY KEY,
    vendor_id VARCHAR(50) NOT NULL,
    station_id VARCHAR(50) NOT NULL,
    charger_id VARCHAR(50) NOT NULL,
    connect_id VARCHAR(50),
    invoice_no VARCHAR(50),
    customer_id VARCHAR(50),
    invoice_date VARCHAR(20),
    carbon_credit VARCHAR(1),
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    kwh VARCHAR(50),
    car_type VARCHAR(10),
    car_tonnage VARCHAR(50),
    car_no VARCHAR(20),
    create_time VARCHAR(20),
    update_time VARCHAR(20)
);
```

#### 5. carbon_station_power (充電站電力表)
```sql
CREATE TABLE carbon_station_power (
    id INT AUTO_INCREMENT PRIMARY KEY,
    station_id VARCHAR(50) NOT NULL,
    station_kwh VARCHAR(50),
    power_load VARCHAR(50),
    timestamp VARCHAR(20),
    create_time VARCHAR(20)
);
```

### 索引設計
```sql
-- 廠商表索引
CREATE INDEX idx_vendor_status ON carbon_vendor(status);
CREATE INDEX idx_vendor_role ON carbon_vendor(role);

-- 充電站表索引
CREATE INDEX idx_station_vendor ON carbon_station(vendor_id);
CREATE INDEX idx_station_status ON carbon_station(station_status);
CREATE INDEX idx_station_city ON carbon_station(city);

-- 充電樁表索引
CREATE INDEX idx_charger_station ON carbon_charger(station_id);
CREATE INDEX idx_charger_vendor ON carbon_charger(vendor_id);
CREATE INDEX idx_charger_status ON carbon_charger(charger_status);

-- 充電記錄表索引
CREATE INDEX idx_charge_vendor ON carbon_charge(vendor_id);
CREATE INDEX idx_charge_station ON carbon_charge(station_id);
CREATE INDEX idx_charge_charger ON carbon_charge(charger_id);
CREATE INDEX idx_charge_start_time ON carbon_charge(start_time);
CREATE INDEX idx_charge_carbon_credit ON carbon_charge(carbon_credit);
```

---

## 🔐 安全規格

### 認證機制
- **JWT Token認證**: 基於JSON Web Token的無狀態認證
- **Token有效期**: 12小時 (43200秒)
- **Token刷新**: 支援自動刷新機制
- **密碼加密**: BCrypt加密算法

### 授權控制
- **角色基礎訪問控制 (RBAC)**:
  - ROLE_ADMIN: 系統管理員權限
  - ROLE_USER: 普通用戶權限
- **資源訪問控制**: 用戶只能訪問自己的數據
- **API權限控制**: 基於Spring Security的註解控制

### 數據安全
- **SQL注入防護**: 使用JPA參數化查詢
- **XSS防護**: 輸入驗證和輸出編碼
- **CSRF防護**: Spring Security CSRF保護
- **敏感數據加密**: 密碼等敏感信息加密存儲

### 安全配置
```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // 安全配置實現
}
```

---

## 🎨 前端規格

### 技術棧
- **模板引擎**: Thymeleaf 3.0
- **CSS框架**: Bootstrap 4.6.0
- **JavaScript庫**: jQuery, DataTables
- **圖表庫**: Chart.js
- **地圖API**: Google Maps API

### 響應式設計
- **斷點設置**:
  - 手機: < 768px
  - 平板: 768px - 1024px
  - 桌面: > 1024px
- **流體佈局**: 使用Bootstrap網格系統
- **觸控友好**: 支援觸控設備操作

### 用戶界面設計
- **設計主題**: 環保綠色主題
- **色彩方案**:
  - 主色: #00A170 (綠色)
  - 輔色: #88B04B (葉綠色)
  - 強調色: #34B3A9 (藍綠色)
- **字體**: Roboto, Noto Sans TC
- **圖標**: Font Awesome 5.15.3

### 國際化支援
- **支援語言**: 繁體中文、英文
- **語言切換**: 動態語言切換功能
- **本地化資源**: 外部化消息文件
- **日期格式**: 本地化日期顯示

### 前端功能模組

#### 1. 認證模組
- 登錄頁面
- 註冊頁面
- 密碼重置
- 會話管理

#### 2. 數據展示模組
- 數據表格 (DataTables)
- 分頁功能
- 搜索篩選
- 排序功能

#### 3. 圖表模組
- 統計圖表
- 趨勢分析
- 實時數據更新

#### 4. 地圖模組
- 充電站地圖
- 位置標記
- 互動功能

---

## 🚀 部署規格

### 部署環境
- **操作系統**: Linux (Ubuntu 20.04+)
- **Java版本**: OpenJDK 8 或 Oracle JDK 8
- **Web服務器**: 內嵌Tomcat
- **數據庫**: MySQL 8.0
- **反向代理**: Nginx (可選)

### 部署配置
```properties
# 服務器配置
server.port=9095
server.servlet.context-path=/carbon

# 數據庫配置
spring.datasource.url=jdbc:mysql://host:3306/database
spring.datasource.username=username
spring.datasource.password=password

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT配置
jwt.secret=your-secret-key
jwt.expiration=43200000

# 日誌配置
logging.level.root=INFO
logging.level.com.carbon=INFO
```

### 容器化部署
```dockerfile
FROM openjdk:8-jre-alpine
COPY target/carbon-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9095
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 監控配置
- **應用監控**: Spring Boot Actuator
- **日誌管理**: Logback + ELK Stack
- **性能監控**: JVM監控 + 數據庫監控
- **告警機制**: 異常告警 + 性能告警

---

## 🧪 測試規格

### 單元測試
- **測試框架**: JUnit 5 + Mockito
- **測試覆蓋率**: 目標80%以上
- **測試範圍**: Service層、Repository層
- **測試數據**: 使用H2內存數據庫

### 集成測試
- **測試框架**: Spring Boot Test
- **測試範圍**: Controller層、API端點
- **測試環境**: 測試數據庫
- **測試數據**: 預設測試數據

### 端到端測試
- **測試工具**: Selenium WebDriver
- **測試範圍**: 用戶界面流程
- **測試場景**: 主要業務流程
- **測試報告**: HTML報告

### 性能測試
- **測試工具**: JMeter
- **測試指標**: 響應時間、吞吐量
- **測試場景**: 高併發、大數據量
- **性能基準**: 1000 TPS

### API測試
- **測試工具**: Postman
- **測試範圍**: 所有API端點
- **測試數據**: 預設測試數據集
- **自動化**: CI/CD集成

---

## 🔧 維護規格

### 日誌管理
- **日誌框架**: Logback
- **日誌級別**: ERROR, WARN, INFO, DEBUG
- **日誌格式**: 結構化日誌
- **日誌輪轉**: 按大小和時間輪轉

### 備份策略
- **數據庫備份**: 每日全量備份
- **增量備份**: 每小時增量備份
- **備份保留**: 保留30天
- **恢復測試**: 每月恢復測試

### 監控告警
- **系統監控**: CPU、記憶體、磁盤
- **應用監控**: JVM、數據庫連接
- **業務監控**: API響應時間、錯誤率
- **告警機制**: 郵件、短信告警

### 版本管理
- **版本控制**: Git
- **分支策略**: Git Flow
- **發布流程**: 自動化部署
- **回滾機制**: 快速回滾能力

### 文檔維護
- **API文檔**: Swagger/OpenAPI
- **用戶手冊**: 在線幫助文檔
- **開發文檔**: 技術文檔
- **部署文檔**: 部署指南

---

## 📊 性能規格

### 響應時間要求
- **頁面加載**: < 3秒
- **API響應**: < 1秒
- **數據庫查詢**: < 500ms
- **文件上傳**: < 10秒 (10MB)

### 併發處理能力
- **同時在線用戶**: 1000+
- **API併發請求**: 500 TPS
- **數據庫連接池**: 50-100連接
- **會話管理**: 無狀態設計

### 數據處理能力
- **數據庫容量**: 1TB+
- **單表記錄數**: 1000萬+
- **查詢優化**: 索引優化
- **分頁處理**: 虛擬滾動

### 可用性要求
- **系統可用性**: 99.9%
- **故障恢復時間**: < 30分鐘
- **數據一致性**: 強一致性
- **災難恢復**: RTO < 4小時

---

## 📋 驗收標準

### 功能驗收
- [ ] 所有功能模組正常運行
- [ ] 用戶認證和授權正確
- [ ] 數據CRUD操作正常
- [ ] API接口符合規格
- [ ] 前端界面響應正常

### 性能驗收
- [ ] 響應時間符合要求
- [ ] 併發處理能力達標
- [ ] 數據庫性能優化
- [ ] 系統穩定性良好

### 安全驗收
- [ ] 認證機制安全可靠
- [ ] 授權控制正確實施
- [ ] 數據安全保護到位
- [ ] 安全漏洞掃描通過

### 兼容性驗收
- [ ] 多瀏覽器兼容
- [ ] 多設備響應式
- [ ] 多語言支援
- [ ] 數據庫兼容性

---

## 📝 附錄

### A. 開發環境搭建
1. 安裝JDK 8
2. 安裝Maven 3.6+
3. 安裝MySQL 8.0
4. 配置IDE (IntelliJ IDEA)
5. 克隆專案代碼
6. 配置數據庫連接
7. 運行應用程序

### B. 常用命令
```bash
# 編譯專案
mvn clean compile

# 運行測試
mvn test

# 打包應用
mvn package

# 運行應用
java -jar target/carbon-0.0.1-SNAPSHOT.jar

# 數據庫遷移
mvn flyway:migrate
```

### C. 故障排除
- **常見問題**: 連接數據庫失敗、JWT Token過期
- **解決方案**: 檢查配置、重啟服務、清除緩存
- **日誌查看**: 查看應用日誌和數據庫日誌
- **性能調優**: JVM參數調優、數據庫查詢優化

---

**文檔版本**: v1.0  
**最後更新**: 2025年1月  
**維護人員**: 開發團隊  
**審核狀態**: 待審核 