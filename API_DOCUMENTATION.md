# 碳權交易平台 - API 文檔

## 📋 API 概覽

**基礎 URL**: `http://localhost:9095/carbon/api/vendor`

**認證方式**: JWT Bearer Token（除取得Token API外）

**Content-Type**: `application/json`

---

## 🔐 認證相關 API

### 1. 取得認證 Token

**端點**: `POST /GetToken`

**描述**: 取得JWT認證Token，用於後續API調用

**認證需求**: ❌ 無需認證

#### 請求參數
```json
{
  "vendorId": "string",
  "password": "string"
}
```

#### 請求範例
```bash
curl -X POST http://localhost:9095/carbon/api/vendor/GetToken \
  -H "Content-Type: application/json" \
  -d '{
    "vendorId": "VENDOR001",
    "password": "your_password"
  }'
```

#### 成功回應 (200)
```json
{
  "success": true,
  "message": "Token generated successfully",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 43200,
  "vendorId": "VENDOR001"
}
```

#### 錯誤回應 (401)
```json
{
  "success": false,
  "message": "Invalid credentials",
  "errorCode": "INVALID_CREDENTIALS"
}
```



---

## 🔌 充電站管理 API



### 2. 新增/更新充電站資訊

**端點**: `POST /StationInfo`

**描述**: 新增或更新充電站場域資訊

**認證需求**: ✅ 需要 Bearer Token

#### 請求參數
```json
{
  "vendorId": "string",
  "stationAry": [
    {
      "stationId": "string",
      "stationKwh": "string",
      "city": "string",
      "address": "string",
      "storageEnable": "string",
      "powerSource": "string",
      "gpsLatitude": "string",
      "gpsLongitude": "string",
      "stationStatus": "string",
      "updateStatus": "string"
    }
  ]
}
```

#### 請求範例
```bash
curl -X POST http://localhost:9095/carbon/api/vendor/StationInfo \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d @station_info_request_example.json
```

#### 範例請求內容
```json
{
  "vendorId": "VENDOR001",
  "stationAry": [
    {
      "stationId": "STATION001",
      "stationKwh": "500",
      "city": "台北市",
      "address": "信義區信義路五段7號",
      "storageEnable": "Y",
      "powerSource": "Grid",
      "gpsLatitude": "25.0339",
      "gpsLongitude": "121.5644",
      "stationStatus": "A",
      "updateStatus": "U"
    }
  ]
}
```

#### 成功回應 (200)
```json
{
  "resultCode": "0000",
  "resultMsg": "充電站資訊更新成功"
}
```

---



## ⚡ 充電樁管理 API



### 3. 新增/更新充電樁資訊

**端點**: `POST /ChargerInfo`

**描述**: 新增或更新充電樁設備資訊

**認證需求**: ✅ 需要 Bearer Token

#### 請求參數
```json
{
  "vendorId": "string",
  "stationId": "string",
  "chargerAry": [
    {
      "chargerId": "string",
      "chargerType": "string",
      "chargerBrand": "string",
      "chargerStatus": "string",
      "updateStatus": "string",
      "chargerKw": "string",
      "chargerA": "string",
      "chargerV": "string",
      "connectorAry": [
        {
          "connectId": "string",
          "connectorType": "string"
        }
      ]
    }
  ]
}
```

#### 請求範例
```bash
curl -X POST http://localhost:9095/carbon/api/vendor/ChargerInfo \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d @charger_info_request_example.json
```

#### 成功回應 (200)
```json
{
  "resultCode": "0000",
  "resultMsg": "充電樁資訊更新成功"
}
```

---



## 📊 電力監控 API



### 4. 新增/更新電力資訊

**端點**: `POST /StationPower`

**描述**: 新增或更新充電站電力使用資訊

**認證需求**: ✅ 需要 Bearer Token

#### 請求參數
```json
{
  "vendorId": "string",
  "stationId": "string",
  "powerUsage": "string",
  "dateInfo": "string",
  "timeInfo": "string"
}
```

#### 請求範例
```bash
curl -X POST http://localhost:9095/carbon/api/vendor/StationPower \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "vendorId": "VENDOR001",
    "stationId": "STATION001",
    "powerUsage": "450.5",
    "dateInfo": "2023-09-01",
    "timeInfo": "14:30:00"
  }'
```

#### 成功回應 (200)
```json
{
  "resultCode": "0000",
  "resultMsg": "電力資訊更新成功"
}
```

---

## 🔋 充電記錄 API



### 5. 新增/更新充電記錄

**端點**: `POST /ChargeInfo`

**描述**: 新增或更新充電交易記錄

**認證需求**: ✅ 需要 Bearer Token

#### 請求參數
```json
{
  "vendorId": "string",
  "transAry": [
    {
      "stationId": "string",
      "chargerId": "string",
      "connectId": "string",
      "transNo": "string",
      "invoiceNo": "string",
      "startTime": "string",
      "endTime": "string",
      "kwh": "string",
      "carType": "string",
      "carTonnage": "string",
      "carNo": "string"
    }
  ]
}
```

#### 請求範例
```bash
curl -X POST http://localhost:9095/carbon/api/vendor/ChargeInfo \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d @charge_info_request_example.json
```

#### 成功回應 (200)
```json
{
  "resultCode": "0000",
  "resultMsg": "充電記錄更新成功"
}
```

---





## 📝 資料字典

### 充電站狀態 (stationStatus)
| 值 | 描述 |
|----|------|
| `A` | 啟用 (Active) |
| `I` | 停用 (Inactive) |

### 充電樁狀態 (chargerStatus)
| 值 | 描述 |
|----|------|
| `A` | 啟用 (Active) |
| `I` | 停用 (Inactive) |
| `M` | 維護中 (Maintenance) |

### 充電樁類型 (chargerType)
| 值 | 描述 |
|----|------|
| `AC` | 交流充電 |
| `DC` | 直流充電 |

### 充電槍類型 (connectorType)
| 值 | 描述 |
|----|------|
| `Type2` | Type2 (歐規AC) |
| `CCS2` | CCS2 (歐規DC) |
| `CHAdeMO` | CHAdeMO (日規DC) |
| `Tesla` | Tesla 專用 |

### 更新狀態 (updateStatus)
| 值 | 描述 |
|----|------|
| `U` | 新增或更新 (Update) |
| `D` | 刪除 (Delete) |

### 儲能設備 (storageEnable)
| 值 | 描述 |
|----|------|
| `Y` | 有儲能設備 |
| `N` | 無儲能設備 |

### 電源來源 (powerSource)
| 值 | 描述 |
|----|------|
| `Grid` | 市電 |
| `Solar` | 太陽能 |
| `Wind` | 風力 |
| `Hybrid` | 混合電源 |

---

## ⚠️ 錯誤代碼

### 通用錯誤回應格式
```json
{
  "resultCode": "error_code",
  "resultMsg": "錯誤訊息描述",
  "timestamp": "2023-09-01T08:30:00",
  "path": "/api/vendor/endpoint"
}
```

### 常見錯誤代碼
| 代碼 | HTTP狀態 | 描述 |
|------|----------|------|
| `0000` | 200 | 成功 |
| `1001` | 400 | 請求參數錯誤 |
| `1002` | 401 | 認證失敗 |
| `1003` | 403 | 權限不足 |
| `1004` | 404 | 資源不存在 |
| `1005` | 409 | 資料衝突 |
| `5001` | 500 | 內部伺服器錯誤 |
| `5002` | 503 | 服務暫時不可用 |

---

## 🔧 開發工具

### Postman Collection
建議使用 Postman 進行API測試，可以匯入以下環境變數：

```json
{
  "base_url": "http://localhost:9095/carbon/api/vendor",
  "jwt_token": "YOUR_JWT_TOKEN_HERE",
  "vendor_id": "VENDOR001"
}
```

### cURL 範例腳本
```bash
#!/bin/bash

# 設定基礎變數
BASE_URL="http://localhost:9095/carbon/api/vendor"
VENDOR_ID="VENDOR001"
PASSWORD="your_password"

# 1. 取得 Token
TOKEN_RESPONSE=$(curl -s -X POST "${BASE_URL}/GetToken" \
  -H "Content-Type: application/json" \
  -d "{\"vendorId\":\"${VENDOR_ID}\",\"password\":\"${PASSWORD}\"}")

# 提取 Token
TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')

# 2. 使用 Token 查詢充電站資訊
curl -X GET "${BASE_URL}/StationInfo" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

---

## 📞 技術支援

- **API版本**: v1.0
- **文檔更新**: 2024年12月
- **支援郵箱**: api-support@carbon-platform.com

如有API相關問題，請提供：
1. API端點和請求方法
2. 完整的請求頭和參數
3. 錯誤回應內容
4. 預期的行為描述

**最後更新**: 2024年12月
**文檔版本**: v1.0 