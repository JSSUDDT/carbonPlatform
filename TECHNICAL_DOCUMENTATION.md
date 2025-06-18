# 碳權交易平台 - 技術文檔

## 專案概覽
**碳權交易平台** 是一個基於 Spring Boot 的充電站管理系統，提供完整的充電站、充電樁管理功能，包含前端網頁界面和後端API服務。

---

## 🔧 後端（Backend）技術與使用說明

### 技術架構
- **框架**: Spring Boot 2.6.15
- **Java版本**: JDK 8
- **數據庫**: MySQL 8.x (Azure Cloud)
- **安全框架**: Spring Security + JWT
- **ORM框架**: Spring Data JPA
- **模板引擎**: Thymeleaf
- **建構工具**: Maven 3.x

### 核心依賴
```xml
<dependencies>
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
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
</dependencies>
```

### 應用配置
- **端口**: 9095
- **上下文路徑**: `/carbon`
- **JWT有效期**: 12小時
- **數據庫**: Azure MySQL雲端數據庫
- **編碼**: UTF-8 全域支援

### 數據模型（Entity）
| 實體類 | 描述 | 主要欄位 |
|--------|------|----------|
| `CarbonVendor` | 廠商資訊管理 | vendorId, vendorName, email |
| `CarbonStation` | 充電站場域資訊 | stationId, stationName, address |
| `CarbonCharger` | 充電樁和充電槍統一管理 | chargerId, stationId, connectorType |
| `CarbonCharge` | 充電交易記錄 | chargeId, stationId, startTime, endTime |
| `CarbonStationPower` | 充電站電力監控數據 | stationId, powerUsage, dateInfo |

### 主要API端點
| 端點 | 方法 | 描述 | 認證需求 |
|------|------|------|----------|
| `/api/vendor/GetToken` | POST | 取得JWT認證Token | ❌ |
| `/api/vendor/StationInfo` | POST | 充電站場域資訊CRUD | ✅ |
| `/api/vendor/ChargerInfo` | POST | 充電樁資訊CRUD | ✅ |
| `/api/vendor/StationPower` | POST | 場域負載資訊傳送 | ✅ |
| `/api/vendor/ChargeInfo` | POST | 交易資訊上傳 | ✅ |

### 安全機制
- **JWT認證**: Bearer Token方式
- **角色管理**: ADMIN、USER角色區分
- **API保護**: 除登入外所有API都需要Token
- **CORS配置**: 支援跨域請求

### 國際化支援
- **多語言**: 中文(zh_TW)、英文(en)
- **資源文件**: messages.properties系列
- **動態切換**: 前端即時語言切換

### 啟動方式
```bash
# 1. 確保Java 8+和Maven已安裝
java -version
mvn -version

# 2. 編譯專案
mvn clean package

# 3. 運行應用
java -jar target/carbon-0.0.1-SNAPSHOT.jar

# 4. 訪問地址
http://localhost:9095/carbon/
```

---

## 💻 前端（Frontend）技術與使用說明

### 技術架構
- **模板引擎**: Thymeleaf 3.x
- **CSS框架**: Bootstrap 4.6.0
- **JavaScript庫**: jQuery 3.x
- **表格組件**: DataTables 1.10.24
- **地圖庫**: Leaflet 1.9.4 (開源)
- **圖示庫**: Font Awesome 5.15.3
- **日期選擇**: DateRangePicker
- **圖表庫**: Chart.js

### 頁面結構

#### 認證相關頁面
| 頁面 | 檔案 | 描述 | 功能 |
|------|------|------|------|
| 登入頁面 | `login.html` | 用戶登入驗證 | 登入、記住狀態、語言切換 |
| 註冊頁面 | `register.html` | 新用戶註冊 | 註冊、驗證、自動跳轉 |

#### 主要功能頁面
| 頁面 | 檔案 | 描述 | 主要功能 |
|------|------|------|----------|
| 充電記錄 | `transactions.html` | 充電交易記錄查詢 | 搜尋、篩選、日期查詢 |
| 充電站管理 | `stations.html` | 充電站資訊CRUD | 管理、狀態更新、權限控制 |
| 充電站地圖 | `stationmap.html` | Leaflet地圖顯示 | 地圖、標記、搜尋定位 |
| 充電樁管理 | `chargers.html` | 充電樁設備管理 | 設備管理、狀態監控 |
| 充電站電力 | `stationpower.html` | 電力使用監控 | 監控、圖表、統計 |

#### 共用組件
| 組件 | 檔案 | 描述 |
|------|------|------|
| 側邊欄 | `sidebar.html` | 導航選單、權限控制、響應式設計 |

### 前端功能特色

#### 🗺️ 地圖功能（Leaflet）
- **開源免費**: 無需API Key
- **輕量高效**: 檔案小、載入快
- **自定義標記**: 不同狀態顯示不同顏色
- **互動彈窗**: 點擊查看充電站詳細資訊
- **搜尋定位**: 輸入充電站名稱自動定位
- **圖層支援**: 預設使用OpenStreetMap

#### 📊 數據表格功能
- **DataTables**: 專業表格組件
- **即時搜尋**: 輸入即時過濾結果
- **分頁顯示**: 自動分頁處理
- **欄位排序**: 點擊表頭排序
- **響應式設計**: 支援行動裝置
- **多語言**: 表格介面中英切換
- **⚠️ 注意**: 目前不支援Excel/PDF匯出功能

#### 🔍 搜尋與篩選
- **全字段搜尋**: 搜尋所有欄位內容
- **日期區間**: DateRangePicker組件
- **下拉篩選**: 充電站、狀態等條件
- **組合篩選**: 多條件同時篩選
- **一鍵重置**: 清空所有篩選條件

#### 🎨 用戶體驗
- **多語言**: 繁體中文/英文即時切換
- **響應式**: 適配桌面、平板、手機
- **載入動畫**: Ajax請求時顯示載入提示
- **錯誤處理**: 友善的錯誤訊息提示
- **權限控制**: 根據用戶角色顯示功能

### 靜態資源結構
```
/static/
├── css/
│   └── styles.css           # 自定義樣式
├── js/
│   ├── common.js           # 共用JavaScript功能
│   ├── locale.js           # 多語言支援
│   └── datatables/         # DataTables語言包
│       └── i18n/
│           ├── Chinese-traditional.json
│           └── English.json
└── images/                 # 圖片資源
```

### 瀏覽器支援
- ✅ Chrome 60+
- ✅ Firefox 55+
- ✅ Safari 12+
- ✅ Edge 79+

---

## 🚀 部署與開發

### 環境要求
- **後端**: JDK 8+, Maven 3.x, MySQL 8.x
- **前端**: 現代瀏覽器（支援ES6）
- **開發工具**: IntelliJ IDEA / VS Code

### 本地開發設置

#### 1. 資料庫配置
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/carbon_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

#### 2. JWT密鑰更新
```properties
jwt.secret=your_secret_key_here
jwt.expiration=43200000
```

#### 3. 啟動除錯模式
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生產環境建議
- **HTTPS**: 使用SSL憑證
- **資料庫**: 使用連接池和讀寫分離
- **快取**: 考慮加入Redis快取
- **監控**: 整合APM監控工具
- **日誌**: 使用集中式日誌管理

### Git版本管理
```bash
# 查看當前倉庫狀態
git status

# 推送到GitHub
git push origin main

# 建立功能分支
git checkout -b feature/new-feature

# 合併分支
git checkout main
git merge feature/new-feature
```

---

## 🧪 API測試

### 測試工具
專案提供完整的API測試範例：
- `station_info_request_example.json` - 充電站資訊測試
- `charger_info_request_example.json` - 充電樁資訊測試
- `charge_info_request_example.json` - 充電記錄測試
- `example_transaction_request.json` - 交易資訊測試

### 測試流程
1. **取得Token**
   ```bash
   curl -X POST http://localhost:9095/carbon/api/vendor/GetToken \
   -H "Content-Type: application/json" \
   -d '{"vendorId": "your_vendor_id", "password": "your_password"}'
   ```

2. **使用Token調用API**
   ```bash
   curl -X POST http://localhost:9095/carbon/api/vendor/StationInfo \
   -H "Authorization: Bearer YOUR_JWT_TOKEN" \
   -H "Content-Type: application/json" \
   -d @station_info_request_example.json
   ```

---

## ⚠️ 已知限制與注意事項

### 功能限制
- ❌ 不支援Excel/PDF匯出功能
- ❌ 地圖服務依賴網路連線
- ⚠️ JWT Token無自動續約機制
- ⚠️ 大量數據載入可能影響效能

### 安全考量
- 🔒 生產環境請更改JWT密鑰
- 🔒 建議使用HTTPS協議
- 🔒 定期更新依賴套件版本
- 🔒 資料庫連接使用SSL

### 效能建議
- 📈 大數據量時考慮分頁載入
- 📈 頻繁查詢可考慮加入快取
- 📈 靜態資源建議使用CDN
- 📈 資料庫查詢優化索引

---

## 📞 技術支援

### 專案維護
- **版本**: v1.0.0
- **最後更新**: 2024年12月
- **維護狀態**: 積極維護

### 問題回報
如遇到技術問題，請提供以下資訊：
1. 錯誤訊息截圖
2. 瀏覽器版本
3. 操作步驟
4. 系統環境

### 功能擴展
如需要增加以下功能，需要額外開發：
- Excel/PDF匯出功能
- 即時推播通知
- 行動APP介面
- 第三方地圖API整合

---

## 📄 授權聲明
本專案為內部開發使用，請遵守相關授權條款。

**最後更新**: 2024年12月
**文件版本**: v1.0 