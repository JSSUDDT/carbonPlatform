# Postman 測試範例 - 批次更新充電站國家資訊

## 📋 目錄
1. [步驟 1: 獲取 JWT Token](#步驟-1-獲取-jwt-token)
2. [步驟 2: 批次更新充電站國家資訊](#步驟-2-批次更新充電站國家資訊)

---

## 步驟 1: 獲取 JWT Token

### 請求設定

**Method:** `POST`

**URL (本地測試):**
```
http://localhost:9095/carbon/api/vendor/GetToken
```
⚠️ **注意**: 根據 `application.properties` 配置：
- 端口: `9095` (不是 8080)
- Context Path: `/carbon`

**URL (生產環境 HTTPS):**
```
https://your-domain.com/api/vendor/GetToken
```
例如：
```
https://appprod.japaneast.cloudapp.azure.com/carbon/api/vendor/GetToken
```

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "vendorId": "your_admin_vendor_id",
  "password": "your_password"
}
```

### Postman 設定步驟

1. 創建新請求，命名為 `1. Get Token`
2. 選擇方法：`POST`
3. 輸入 URL：`{{baseUrl}}/api/vendor/GetToken`
   - 建議在 Postman 環境變數中設定 `baseUrl`
4. 在 **Headers** 標籤頁：
   - Key: `Content-Type`
   - Value: `application/json`
5. 在 **Body** 標籤頁：
   - 選擇 `raw`
   - 選擇 `JSON`
   - 貼上上面的 JSON body
6. 點擊 **Send**

### 預期回應

**成功回應 (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxMTAwMDAwMCwiZXhwIjoxNzExMDg2NDAwLCJyb2xlcyI6IlJPTEVfQURNSU4ifQ.xxxxx",
  "resultCode": "0000"
}
```

**失敗回應 (200 OK):**
```json
{
  "token": null,
  "resultCode": "0001"
}
```

### 自動保存 Token（可選）

在 Postman 的 **Tests** 標籤頁添加以下腳本，自動保存 token：

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.token) {
        pm.environment.set("jwt_token", jsonData.token);
        console.log("Token saved to environment variable");
    }
}
```

---

## 步驟 2: 批次更新充電站國家資訊

### 請求設定

**Method:** `POST`

**URL (本地測試 - 預設模式):**
```
http://localhost:9095/carbon/api/admin/stations/update-country
```

**URL (本地測試 - 強制更新模式):**
```
http://localhost:9095/carbon/api/admin/stations/update-country?forceUpdate=true
```
⚠️ **注意**: 根據 `application.properties` 配置：
- 端口: `9095` (不是 8080)
- Context Path: `/carbon`

**URL (生產環境 HTTPS - 預設模式):**
```
https://your-domain.com/api/admin/stations/update-country
```
例如：
```
https://appprod.japaneast.cloudapp.azure.com/carbon/api/admin/stations/update-country
```

**URL (生產環境 HTTPS - 強制更新模式):**
```
https://your-domain.com/api/admin/stations/update-country?forceUpdate=true
```
例如：
```
https://appprod.japaneast.cloudapp.azure.com/carbon/api/admin/stations/update-country?forceUpdate=true
```

**Headers:**
```
Authorization: Bearer {your_jwt_token}
Content-Type: application/json
```

### Postman 設定步驟

#### 方案 A: 預設模式（只更新沒有國家代碼的站點）

1. 創建新請求，命名為 `2. Update Stations Country (Default)`
2. 選擇方法：`POST`
3. 輸入 URL：`{{baseUrl}}/api/admin/stations/update-country`
4. 在 **Headers** 標籤頁：
   - Key: `Authorization`
   - Value: `Bearer {{jwt_token}}`
     - 如果已設定環境變數，使用 `{{jwt_token}}`
     - 否則手動貼上從步驟 1 獲取的 token
   - Key: `Content-Type`
   - Value: `application/json`
5. 點擊 **Send**

#### 方案 B: 強制更新模式（更新所有站點）

1. 創建新請求，命名為 `2. Update Stations Country (Force Update)`
2. 選擇方法：`POST`
3. 輸入 URL：`{{baseUrl}}/api/admin/stations/update-country?forceUpdate=true`
4. 在 **Headers** 標籤頁：
   - Key: `Authorization`
   - Value: `Bearer {{jwt_token}}`
   - Key: `Content-Type`
   - Value: `application/json`
5. 點擊 **Send**

### 預期回應

**成功回應 (200 OK):**
```json
{
  "resultCode": "0000",
  "resultMsg": "批次更新完成：總共 100 個充電站，已處理 100 個，成功更新 85 個，跳過 10 個，失敗 5 個"
}
```

**權限不足 (403 Forbidden):**
```json
{
  "timestamp": "2025-11-24T09:00:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/admin/stations/update-country"
}
```

**未授權 (401 Unauthorized):**
```json
{
  "timestamp": "2025-11-24T09:00:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/admin/stations/update-country"
}
```

---

## 📝 Postman Collection 匯入格式

### 方式 1: 使用 Postman Collection JSON

創建一個新的 Collection，包含以下兩個請求：

```json
{
  "info": {
    "name": "Station Country Update API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Get Token",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"vendorId\": \"{{admin_vendor_id}}\",\n  \"password\": \"{{admin_password}}\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/vendor/GetToken",
          "host": ["{{baseUrl}}"],
          "path": ["api", "vendor", "GetToken"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "if (pm.response.code === 200) {",
              "    var jsonData = pm.response.json();",
              "    if (jsonData.token) {",
              "        pm.environment.set('jwt_token', jsonData.token);",
              "        console.log('Token saved to environment variable');",
              "    }",
              "}"
            ]
          }
        }
      ]
    },
    {
      "name": "2. Update Stations Country (Default)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/admin/stations/update-country",
          "host": ["{{baseUrl}}"],
          "path": ["api", "admin", "stations", "update-country"]
        }
      }
    },
    {
      "name": "2. Update Stations Country (Force Update)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/admin/stations/update-country?forceUpdate=true",
          "host": ["{{baseUrl}}"],
          "path": ["api", "admin", "stations", "update-country"],
          "query": [
            {
              "key": "forceUpdate",
              "value": "true"
            }
          ]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:9095/carbon"
    },
    {
      "key": "admin_vendor_id",
      "value": "your_admin_vendor_id"
    },
    {
      "key": "admin_password",
      "value": "your_admin_password"
    }
  ]
}
```

### 方式 2: 環境變數設定

在 Postman 中創建環境變數：

1. 點擊右上角的 **環境管理** (齒輪圖示)
2. 創建新環境，命名為 `Carbon API Local` 或 `Carbon API Production`
3. 添加以下變數：

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| `baseUrl` | `http://localhost:9095/carbon` | `http://localhost:9095/carbon` |
| `admin_vendor_id` | `your_admin_vendor_id` | `your_admin_vendor_id` |
| `admin_password` | `your_admin_password` | `your_admin_password` |
| `jwt_token` | (留空，由腳本自動填入) | (留空) |

---

## 🔒 HTTPS 設定說明

### Postman SSL 證書驗證設定

如果使用 HTTPS 且遇到 SSL 證書驗證問題：

1. **關閉 SSL 證書驗證（僅用於測試環境）：**
   - 點擊 Postman 右上角的 **Settings** (齒輪圖示)
   - 在 **General** 標籤頁中
   - 找到 **SSL certificate verification** 選項
   - 關閉此選項（僅在測試環境使用，生產環境不建議）

2. **使用自簽名證書：**
   - 在 **Settings** → **Certificates** 標籤頁
   - 可以添加自簽名證書

3. **HTTPS URL 格式：**
   ```
   https://your-domain.com/carbon/api/vendor/GetToken
   https://your-domain.com/carbon/api/admin/stations/update-country
   ```

### 常見 HTTPS 問題

**問題 1: "SSL Error: Unable to verify the first certificate"**
- **解決方案**: 在 Postman Settings 中關閉 SSL 證書驗證（僅測試環境）

**問題 2: "Error: connect ECONNREFUSED"**
- **解決方案**: 檢查 URL 是否正確，確認伺服器是否運行

**問題 3: "Error: getaddrinfo ENOTFOUND"**
- **解決方案**: 檢查域名是否正確，確認 DNS 解析是否正常

---

## ⚠️ 注意事項

1. **權限要求**: 此 API 需要 `ROLE_ADMIN` 角色，確保使用的帳號具有管理員權限
2. **執行時間**: 根據充電站數量，可能需要較長時間（受 OpenCage API 速率限制影響）
   - 100 個站點約需 100 秒（1 請求/秒）
3. **建議**: 
   - 首次執行使用預設模式（`forceUpdate=false`）
   - 在低峰時段執行
   - 查看應用程式 log 追蹤進度
4. **Token 過期**: JWT token 有過期時間，如果收到 401 錯誤，請重新執行步驟 1 獲取新 token
5. **HTTPS 路徑**: 如果應用部署在 `/carbon` 路徑下，URL 需要包含 `/carbon` 前綴

---

## 🔍 除錯技巧

### 檢查 Token 是否有效

使用測試端點驗證 token：

**URL:** `GET {{baseUrl}}/api/test/validate-token?token={{jwt_token}}`

### 查看詳細 Log

在應用程式 log 中搜尋以下關鍵字：
- `開始批次更新所有充電站的國家資訊`
- `處理充電站`
- `成功更新充電站`
- `進度:`

---

## 📞 支援

如有問題，請檢查：
1. Token 是否有效且未過期
2. 帳號是否具有 `ROLE_ADMIN` 角色
3. API 端點 URL 是否正確
4. 網路連線是否正常

