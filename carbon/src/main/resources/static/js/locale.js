/**
 * 多語言支持JavaScript函數庫
 */

// 當前語言的Cookie名稱
const LOCALE_COOKIE_NAME = 'carbonLocale';

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
}

// 在DOM加載完成後初始化語言選擇器
document.addEventListener('DOMContentLoaded', function() {
    initializeLanguageSelector();
}); 