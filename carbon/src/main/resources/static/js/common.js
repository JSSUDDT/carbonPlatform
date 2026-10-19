/**
 * Carbon系統通用JavaScript函數庫
 * 包含認證、API請求處理等功能
 */

// API基礎URL
const API_BASE_URL = '/carbon/api/vendor';

// Token本地存儲鍵名
const TOKEN_KEY = 'carbon_token';
const USER_ROLE_KEY = 'carbon_user_role';
const VENDOR_ID_KEY = 'carbon_vendor_id';

/**
 * 設置JWT令牌到本地存儲
 * @param {string} token JWT令牌
 */
function setAuthToken(token) {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
}

/**
 * 獲取JWT令牌
 * @returns {string|null} JWT令牌
 */
function getAuthToken() {
    return localStorage.getItem(TOKEN_KEY);
}

/**
 * 設置用戶角色到本地存儲
 * @param {string} role 用戶角色
 */
function setUserRole(role) {
    if (role) {
        localStorage.setItem(USER_ROLE_KEY, role);
    } else {
        localStorage.removeItem(USER_ROLE_KEY);
    }
}

/**
 * 獲取用戶角色
 * @returns {string|null} 用戶角色
 */
function getUserRole() {
    return localStorage.getItem(USER_ROLE_KEY);
}

/**
 * 設置廠商ID到本地存儲
 * @param {string} vendorId 廠商ID
 */
function setVendorId(vendorId) {
    if (vendorId) {
        localStorage.setItem(VENDOR_ID_KEY, vendorId);
    } else {
        localStorage.removeItem(VENDOR_ID_KEY);
    }
}

/**
 * 獲取廠商ID
 * @returns {string|null} 廠商ID
 */
function getVendorId() {
    return localStorage.getItem(VENDOR_ID_KEY);
}

/**
 * 檢查用戶是否已登錄
 * @returns {boolean} 是否已登錄
 */
function isAuthenticated() {
    const token = getAuthToken();
    if (!token) {
        return false;
    }
    
    // 嘗試驗證token是否過期
    try {
        // 解析JWT token
        const parts = token.split('.');
        if (parts.length !== 3) {
            console.error('無效的token格式');
            return false;
        }
        
        // 解碼payload部分
        const payload = JSON.parse(atob(parts[1]));
        
        // 檢查過期時間
        const currentTime = Math.floor(Date.now() / 1000);
        if (payload.exp && payload.exp < currentTime) {
            console.log('Token已過期');
            // 清除過期的token
            logout();
            return false;
        }
        
        return true;
    } catch (error) {
        console.error('驗證token時出錯:', error);
        return false;
    }
}

/**
 * 檢查用戶是否是管理員
 * @returns {boolean} 是否是管理員
 */
function isAdmin() {
    return getUserRole() === 'ROLE_ADMIN';
}

/**
 * 執行API請求
 * @param {string} endpoint API端點
 * @param {string} method HTTP方法
 * @param {Object} data 請求數據
 * @returns {Promise} 請求Promise
 */
async function apiRequest(endpoint, method = 'GET', data = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    const token = getAuthToken();
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const options = {
        method,
        headers
    };
    
    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(url, options);
        
        // 處理401未授權響應（令牌過期或無效）
        if (response.status === 401) {
            logout();
            window.location.href = '/carbon/login';
            return null;
        }
        
        // 處理403禁止訪問響應（無權限）
        if (response.status === 403) {
            showError('您沒有權限執行此操作');
            return null;
        }
        
        return await response.json();
    } catch (error) {
        console.error('API請求失敗:', error);
        showError('請求失敗，請稍後再試');
        return null;
    }
}

/**
 * 登錄處理
 * @param {string} vendorId 廠商ID
 * @param {string} password 密碼
 * @returns {Promise<Object>} 登錄結果
 */
async function login(vendorId, password) {
    showLoading();
    
    const response = await apiRequest('/GetToken', 'POST', {
        vendorId,
        password
    });
    
    hideLoading();
    
    if (response && response.token) {
        setAuthToken(response.token);
        setVendorId(vendorId);
        
        // 獲取用戶角色
        await fetchUserRole();
        
        return { success: true };
    }
    
    if (response && response.resultCode === '0003') {
        const disabledMsg = (typeof getI18nMessage === 'function') 
            ? getI18nMessage('login.disabled', '該廠商帳號已被停用，請聯絡系統管理員')
            : '該廠商帳號已被停用，請聯絡系統管理員';
        return { success: false, message: disabledMsg };
    }
    
    return { success: false, message: '登錄失敗，請檢查您的用戶名和密碼' };
}

/**
 * 註冊處理
 * @param {string} vendorId 廠商ID
 * @param {string} password 密碼
 * @returns {Promise<Object>} 註冊結果
 */
async function register(vendorId, password) {
    showLoading();
    
    const response = await apiRequest('/Register', 'POST', {
        vendorId,
        password,
        role: 'ROLE_USER', // 固定為普通用戶角色
        status: '1' // 默認狀態為啟用
    });
    
    hideLoading();
    
    if (response && response.resultCode === '0000') {
        return { success: true };
    }
    
    return { 
        success: false, 
        message: response?.resultMsg || '註冊失敗，請稍後再試'
    };
}

/**
 * 登出處理
 * @param {boolean} redirect 是否重定向到登錄頁面，默認為true
 */
function logout(redirect = true) {
    console.log('執行登出操作...');
    // 清除所有認證相關的本地存儲
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_ROLE_KEY);
    localStorage.removeItem(VENDOR_ID_KEY);
    
    // 使用設置函數保持一致性
    setAuthToken(null);
    setUserRole(null);
    setVendorId(null);
    
    console.log('所有認證數據已清除');
    
    // 重定向到登錄頁面（如果需要）
    if (redirect) {
        window.location.href = '/carbon/login';
    }
}

/**
 * 獲取用戶角色
 */
async function fetchUserRole() {
    const response = await apiRequest('/UserRole');
    if (response && response.role) {
        setUserRole(response.role);
    }
}

/**
 * 顯示錯誤消息
 * @param {string} message 錯誤消息
 */
function showError(message) {
    if (document.getElementById('errorAlert')) {
        const errorAlert = document.getElementById('errorAlert');
        errorAlert.textContent = message;
        errorAlert.classList.remove('d-none');
        
        // 5秒後自動隱藏
        setTimeout(() => {
            errorAlert.classList.add('d-none');
        }, 5000);
    } else {
        alert(message);
    }
}

/**
 * 顯示成功消息
 * @param {string} message 成功消息
 */
function showSuccess(message) {
    if (document.getElementById('successAlert')) {
        const successAlert = document.getElementById('successAlert');
        successAlert.textContent = message;
        successAlert.classList.remove('d-none');
        
        // 5秒後自動隱藏
        setTimeout(() => {
            successAlert.classList.add('d-none');
        }, 5000);
    } else {
        alert(message);
    }
}

/**
 * 顯示加載中動畫
 */
function showLoading() {
    if (document.getElementById('loadingSpinner')) {
        document.getElementById('loadingSpinner').classList.remove('d-none');
    }
}

/**
 * 隱藏加載中動畫
 */
function hideLoading() {
    if (document.getElementById('loadingSpinner')) {
        document.getElementById('loadingSpinner').classList.add('d-none');
    }
}

/**
 * 格式化日期時間
 * @param {string} dateTimeString 日期時間字符串
 * @returns {string} 格式化後的日期時間
 */
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    return date.toLocaleString('zh-TW');
}

/**
 * 獲取URL參數
 * @param {string} name 參數名
 * @returns {string} 參數值
 */
function getUrlParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

/**
 * 計算今日碳減排量並更新UI
 */
function updateDailyCarbonReduction() {
    // 獲取顯示碳減排量的元素
    const carbonReductionElement = document.getElementById('dailyCarbonReduction');
    if (!carbonReductionElement) return;
    
    // 模擬計算邏輯：實際應用中應該從API獲取真實數據
    // 這裡使用隨機值模擬，範圍在15-35公斤之間
    const today = new Date();
    // 使用日期作為種子，使得同一天顯示相同的數值
    const seed = today.getFullYear() * 10000 + (today.getMonth() + 1) * 100 + today.getDate();
    
    // 基於日期的偽隨機數
    const randomValue = (Math.sin(seed) * 10000) % 20 + 15;
    const carbonReduction = randomValue.toFixed(1);
    
    // 更新顯示
    carbonReductionElement.textContent = carbonReduction;
    
    // 更新進度條
    const progressBar = document.querySelector('.eco-tips .progress-bar');
    if (progressBar) {
        // 計算進度百分比，假設目標是40公斤
        const percentage = Math.min((randomValue / 40) * 100, 100);
        progressBar.style.width = `${percentage}%`;
    }
}

/**
 * 頁面加載時檢查認證狀態
 */
document.addEventListener('DOMContentLoaded', function() {
    // 登錄頁面和註冊頁面不需要檢查認證狀態
    if (window.location.pathname.indexOf('/carbon/login') >= 0 || 
        window.location.pathname.indexOf('/carbon/login.html') >= 0 || 
        window.location.pathname.indexOf('/carbon/register') >= 0 || 
        window.location.pathname.indexOf('/carbon/register.html') >= 0) {
        return;
    }
    
    // 檢查是否已登錄
    if (!isAuthenticated()) {
        window.location.href = '/carbon/login';
        return;
    }
    
    // 設置用戶信息顯示
    if (document.getElementById('currentUser')) {
        document.getElementById('currentUser').textContent = getVendorId();
    }
    
    // 設置側邊欄當前活動項
    const path = window.location.pathname;
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        if (link.getAttribute('href') === path) {
            link.classList.add('active');
        }
    });
    
    // 初始化碳減排顯示
    updateDailyCarbonReduction();
}); 