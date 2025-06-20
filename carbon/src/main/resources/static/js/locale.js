/**
 * 多語言支持JavaScript函數庫
 */

// 當前語言的Cookie名稱
const LOCALE_COOKIE_NAME = 'carbonLocale';

// 翻譯字典
const translations = {
    en: {
        // Station Management
        'Add Station': 'Add Station',
        'Edit Station': 'Edit Station',
        'Station not found': 'Station not found',
        'Station updated successfully': 'Station updated successfully',
        'Station added successfully': 'Station added successfully',
        'Station deleted successfully': 'Station deleted successfully',
        'Unsupported operation type': 'Unsupported operation type',
        'No permission to operate on another vendor\'s station': 'No permission to operate on another vendor\'s station',
        'Station does not exist': 'Station does not exist',
        'Unknown error': 'Unknown error',
        'Save failed': 'Save failed',
        'Delete failed': 'Delete failed',
        'Are you sure you want to delete station': 'Are you sure you want to delete station',
        'This action cannot be undone': 'This action cannot be undone!',
        
        // Charger Management
        'Add Charger': 'Add Charger',
        'Edit Charger': 'Edit Charger',
        'Charger not found': 'Charger not found',
        'Charger updated successfully': 'Charger updated successfully',
        'Charger added successfully': 'Charger added successfully',
        'Charger deleted successfully': 'Charger deleted successfully',
        'Are you sure you want to delete charger': 'Are you sure you want to delete charger',
        
        // General
        'Edit': 'Edit',
        'Delete': 'Delete',
        'Yes': 'Yes',
        'No': 'No',
        'Enabled': 'Enabled',
        'Disabled': 'Disabled',
        'Green Energy': 'Green Energy',
        'Traditional Grid': 'Traditional Grid',
        'AC Charger': 'AC Charger',
        'DC Charger': 'DC Charger',
        'Vehicle Information': 'Vehicle Information',
        'All Stations': 'All Stations',
        'Please select vendor': 'Please select vendor',
        'Please select station': 'Please select station',
        'Failed to load data': 'Failed to load data',
        'Failed to load station data': 'Failed to load station data',
        'Failed to load charger data': 'Failed to load charger data',
        'Failed to load transaction data': 'Failed to load transaction data',
        'Failed to load vendor list': 'Failed to load vendor list',
        'Failed to check user role': 'Failed to check user role',
        'Failed to load station list': 'Failed to load station list',
        'Failed to load station power data': 'Failed to load station power data',
        'Failed to load station map data': 'Failed to load station map data'
    },
    zh_TW: {
        // Station Management
        'Add Station': '新增充電站',
        'Edit Station': '編輯充電站',
        'Station not found': '找不到指定的充電站',
        'Station updated successfully': '充電站更新成功',
        'Station added successfully': '充電站新增成功',
        'Station deleted successfully': '充電站刪除成功',
        'Unsupported operation type': '不支持的操作類型',
        'No permission to operate on another vendor\'s station': '無權限操作其他廠商的充電站',
        'Station does not exist': '充電站不存在',
        'Unknown error': '未知錯誤',
        'Save failed': '保存失敗',
        'Delete failed': '刪除失敗',
        'Are you sure you want to delete station': '您確定要刪除充電站',
        'This action cannot be undone': '此操作無法撤銷！',
        
        // Charger Management
        'Add Charger': '新增充電樁',
        'Edit Charger': '編輯充電樁',
        'Charger not found': '找不到指定的充電樁',
        'Charger updated successfully': '充電樁更新成功',
        'Charger added successfully': '充電樁新增成功',
        'Charger deleted successfully': '充電樁刪除成功',
        'Are you sure you want to delete charger': '您確定要刪除充電樁',
        
        // General
        'Edit': '編輯',
        'Delete': '刪除',
        'Yes': '是',
        'No': '否',
        'Enabled': '啟用',
        'Disabled': '禁用',
        'Green Energy': '綠能',
        'Traditional Grid': '傳統電網',
        'AC Charger': '交流充電樁',
        'DC Charger': '直流充電樁',
        'Vehicle Information': '車輛資訊',
        'All Stations': '全部充電站',
        'Please select vendor': '請選擇廠商',
        'Please select station': '請選擇充電站',
        'Failed to load data': '載入資料失敗',
        'Failed to load station data': '載入充電站資料失敗',
        'Failed to load charger data': '載入充電樁資料失敗',
        'Failed to load transaction data': '載入充電記錄資料失敗',
        'Failed to load vendor list': '載入廠商清單失敗',
        'Failed to check user role': '檢查使用者角色失敗',
        'Failed to load station list': '載入充電站清單失敗',
        'Failed to load station power data': '載入充電站電力資料失敗',
        'Failed to load station map data': '載入充電站地圖資料失敗'
    }
};

/**
 * 翻譯文字
 * @param {string} key 翻譯鍵
 * @returns {string} 翻譯後的文字
 */
function translate(key) {
    const currentLang = getCurrentLanguage();
    const langTranslations = translations[currentLang] || translations.en;
    return langTranslations[key] || key;
}

/**
 * 設置語言偏好，更新Cookie並重載頁面
 * @param {string} lang 語言代碼，如'en'、'zh_TW'
 */
function setLanguage(lang) {
    console.log('切換語言到:', lang);
    
    // 設定Cookie
    document.cookie = `${LOCALE_COOKIE_NAME}=${lang}; max-age=${60 * 60 * 24 * 365}; path=/`;
    
    // 重新加載頁面應用新語言設置
    window.location.reload();
}

/**
 * 獲取當前語言設置
 * @returns {string} 當前語言代碼
 */
function getCurrentLanguage() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === LOCALE_COOKIE_NAME) {
            return value;
        }
    }
    return 'en'; // 默認為英文
}

/**
 * 初始化語言選擇器
 */
function initializeLanguageSelector() {
    const currentLang = getCurrentLanguage();
    console.log('當前語言:', currentLang);
    
    // 更新語言選擇器UI（如果存在）
    const langSelector = document.getElementById('languageSelector');
    if (langSelector) {
        langSelector.value = currentLang;
        
        // 添加更改事件監聽器
        langSelector.addEventListener('change', function() {
            setLanguage(this.value);
        });
    }
    
    // 為語言切換按鈕添加事件監聽器
    document.querySelectorAll('.language-switch-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const lang = this.getAttribute('data-lang');
            if (lang) {
                setLanguage(lang);
            }
        });
    });
    
    // 設置日期輸入框的語言
    setDateInputLanguage(currentLang);
}

/**
 * 設置日期輸入框的語言
 * @param {string} lang 語言代碼
 */
function setDateInputLanguage(lang) {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        // 設置 lang 屬性來影響日期選擇器的語言
        input.setAttribute('lang', lang === 'en' ? 'en-US' : 'zh-TW');
        
        // 對於某些瀏覽器，我們可以設置 locale 屬性
        if (lang === 'en') {
            input.setAttribute('data-locale', 'en-US');
        } else {
            input.setAttribute('data-locale', 'zh-TW');
        }
    });
}

// 在DOM加載完成後初始化語言選擇器
document.addEventListener('DOMContentLoaded', function() {
    initializeLanguageSelector();
}); 