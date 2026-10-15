# ===================================================================
# 碳權交易平台 - 遠端伺服器一鍵部署工具 (PowerShell CLI)
# 支援環境: 測試環境 (SIT) / 正式環境 (PROD)
# ===================================================================
param(
    [string]$Env = "",
    [switch]$SkipBuild,
    [switch]$OnlyCheck,
    [switch]$RestartOnly,
    [switch]$StopOnly,
    [switch]$TailLog,
    [switch]$Force
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# ==================== 環境定義 ====================
$Environments = @{
    "sit" = @{
        Id          = "sit"
        Name        = "測試環境 (SIT)"
        Host        = "61.91.11.227"
        Port        = "4936"
        User        = "etreego"
        Pass        = 'CosQQ$154as'
        Key         = $null
        RemoteDir   = "/opt/sit/carbonPlatform"
        Profile     = "sit"
        SyncNote    = "關閉 (停用自動排程拉取，專供外部業者上傳與瀏覽)"
        WebUrl      = "http://61.91.11.227:9095/carbon/login"
        IsProd      = $false
    }
    "prod" = @{
        Id          = "prod"
        Name        = "正式環境 (PROD)"
        Host        = "52.185.184.91"
        Port        = "22"
        User        = "gochabar"
        Pass        = $null
        Key         = "D:\vsCodeProjects\APP-Prod_key\APP-Prod_key.ppk"
        RemoteDir   = "/opt/sit/carbonPlatform"
        Profile     = "prod"
        SyncNote    = "開啟 (定時排程同步拉取外部資料庫)"
        WebUrl      = "http://52.185.184.91:9095/carbon/login"
        IsProd      = $true
    }
}

# ==================== 本地路徑偵測 ====================
$CurrentDir = $PSScriptRoot
if (Test-Path (Join-Path $CurrentDir "carbon\pom.xml")) {
    $CarbonDir = Join-Path $CurrentDir "carbon"
} elseif (Test-Path (Join-Path $CurrentDir "pom.xml")) {
    $CarbonDir = $CurrentDir
} else {
    $CarbonDir = "d:\vsCodeProjects\carbon_0512\carbon"
}

$LocalJar = Join-Path $CarbonDir "target\carbon-0.0.1-SNAPSHOT.jar"

# ==================== 工具檢查 ====================
function Find-Tool {
    param([string]$Name, [string[]]$Fallbacks)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($fb in $Fallbacks) {
        if (Test-Path $fb) { return $fb }
    }
    return $null
}

$Plink = Find-Tool "plink.exe" @("C:\Program Files\PuTTY\plink.exe", "C:\Program Files (x86)\PuTTY\plink.exe")
$Pscp  = Find-Tool "pscp.exe"  @("C:\Program Files\PuTTY\pscp.exe",  "C:\Program Files (x86)\PuTTY\pscp.exe")

if (-not $Plink -or -not $Pscp) {
    Write-Host "[錯誤] 找不到 plink.exe 或 pscp.exe！請確認已安裝 PuTTY (預設安裝於 C:\Program Files\PuTTY)。" -ForegroundColor Red
    exit 1
}

# 執行遠端 SSH 命令函式
function Invoke-RemoteSSH {
    param(
        [hashtable]$TargetEnv,
        [string]$Command,
        [bool]$Silent = $false
    )
    $cmdArgs = @("-batch", "-P", $TargetEnv.Port, "-l", $TargetEnv.User)
    if ($TargetEnv.Key) {
        $cmdArgs += @("-i", $TargetEnv.Key)
    } elseif ($TargetEnv.Pass) {
        $cmdArgs += @("-pw", $TargetEnv.Pass)
    }
    $cmdArgs += @($TargetEnv.Host, $Command)

    if ($Silent) {
        $output = & $Plink @cmdArgs 2>$null
    } else {
        $output = & $Plink @cmdArgs
    }
    $global:LastSSHExitCode = $LASTEXITCODE
    return $output
}

# 執行遠端 SCP 傳輸函式
function Invoke-RemoteSCP {
    param(
        [hashtable]$TargetEnv,
        [string]$LocalFile,
        [string]$RemoteFile
    )
    $cmdArgs = @("-batch", "-P", $TargetEnv.Port)
    if ($TargetEnv.Key) {
        $cmdArgs += @("-i", $TargetEnv.Key)
    } elseif ($TargetEnv.Pass) {
        $cmdArgs += @("-pw", $TargetEnv.Pass)
    }
    $cmdArgs += @($LocalFile, "$($TargetEnv.User)@$($TargetEnv.Host):$RemoteFile")

    & $Pscp @cmdArgs
    return $LASTEXITCODE
}

# ==================== 環境選擇 ====================
$SelectedEnvKey = $Env.ToLower().Trim()
if (-not ($Environments.ContainsKey($SelectedEnvKey))) {
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "         🌿 碳權交易平台 - 遠端自動化發布系統               " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "請選擇部署目標環境 (5 秒後將預設執行 [1] 測試環境):" -ForegroundColor White
    Write-Host "  [1] 測試環境 (SIT)  - 61.91.11.227:4936 [預設]" -ForegroundColor Yellow
    Write-Host "  [2] 正式環境 (PROD) - 52.185.184.91:22" -ForegroundColor Magenta
    Write-Host ""

    $timeoutSec = 5
    $keyHit = $false
    Write-Host -NoNewline "倒數 $timeoutSec 秒 (按 1 或 2，或按 Enter 直接選擇 SIT)... " -ForegroundColor Yellow

    for ($i = $timeoutSec; $i -gt 0; $i--) {
        if ([Console]::KeyAvailable) {
            $keyHit = $true
            break
        }
        Start-Sleep -Milliseconds 1000
        Write-Host -NoNewline "$i " -ForegroundColor Yellow
    }
    Write-Host ""

    if ($keyHit) {
        $keyChar = [Console]::ReadKey($true).KeyChar
        if ($keyChar -eq '2') {
            $SelectedEnvKey = "prod"
        } else {
            $SelectedEnvKey = "sit"
        }
    } else {
        Write-Host "已超時，自動選定 [1] 測試環境 (SIT)..." -ForegroundColor Yellow
        $SelectedEnvKey = "sit"
    }
}

$CurrentEnv = $Environments[$SelectedEnvKey]

# 若為正式環境，進行安全確認提醒
if ($CurrentEnv.IsProd -and -not $Force -and -not $OnlyCheck -and -not $TailLog) {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "  ⚠️  注意：您目前選定的是【正式環境 (PROD)】！" -ForegroundColor Red
    Write-Host "  目標主機: $($CurrentEnv.User)@$($CurrentEnv.Host):$($CurrentEnv.Port)" -ForegroundColor Yellow
    Write-Host "============================================================" -ForegroundColor Red
    $confirm = Read-Host "確定要繼續對正式環境執行操作嗎？(請輸入 YES 繼續，其他鍵取消)"
    if ($confirm -ne "YES") {
        Write-Host "使用者已取消正式環境操作。" -ForegroundColor Gray
        exit 0
    }
}

# ==================== 橫幅資訊 ====================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  發布目標 : $($CurrentEnv.Name)" -ForegroundColor Green
Write-Host "  目標主機 : $($CurrentEnv.User)@$($CurrentEnv.Host):$($CurrentEnv.Port)" -ForegroundColor Gray
Write-Host "  遠端目錄 : $($CurrentEnv.RemoteDir)" -ForegroundColor Gray
Write-Host "  運行配置 : Spring Profile = $($CurrentEnv.Profile)" -ForegroundColor Gray
Write-Host "  排程拉取 : $($CurrentEnv.SyncNote)" -ForegroundColor Gray
Write-Host "  外部網址 : $($CurrentEnv.WebUrl)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ==================== 僅檢查狀態 ====================
if ($OnlyCheck) {
    Write-Host ">>> 正在查詢遠端碳權服務狀態..." -ForegroundColor Yellow
    $chkCmd = "if pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' > /dev/null 2>&1; then echo '【運行中】 PID: '`$(pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' | xargs); netstat -tuln 2>/dev/null | grep 9095 || ss -tuln 2>/dev/null | grep 9095 || true; else echo '【未運行】 目前沒有 Carbon 服務在運行'; fi"
    Invoke-RemoteSSH $CurrentEnv $chkCmd
    Write-Host "`n檢視完畢。" -ForegroundColor Green
    exit 0
}

# ==================== 僅查看日誌 ====================
if ($TailLog) {
    Write-Host ">>> 正在取得遠端最新 30 行日誌..." -ForegroundColor Yellow
    Invoke-RemoteSSH $CurrentEnv "tail -n 30 $($CurrentEnv.RemoteDir)/logs/carbon.log"
    exit 0
}

# ==================== 停止服務 ====================
if ($StopOnly) {
    Write-Host ">>> 正在停止遠端 Carbon 服務..." -ForegroundColor Yellow
    $stopCmd = "pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true; sleep 2; pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true; echo '服務已停止'"
    Invoke-RemoteSSH $CurrentEnv $stopCmd
    exit 0
}

# ==================== 僅重啟服務 ====================
if ($RestartOnly) {
    Write-Host ">>> [1/3] 正在停止既有服務..." -ForegroundColor Yellow
    $stopCmd = "pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true; sleep 2; pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true"
    Invoke-RemoteSSH $CurrentEnv $stopCmd

    Write-Host ">>> [2/3] 正在背景啟動服務 (Profile: $($CurrentEnv.Profile), 時區: Asia/Taipei)..." -ForegroundColor Yellow
    $startCmd = "cd $($CurrentEnv.RemoteDir) && nohup java -Duser.timezone=Asia/Taipei -jar carbon-0.0.1-SNAPSHOT.jar --spring.profiles.active=$($CurrentEnv.Profile) > logs/console.log 2>&1 &"
    Invoke-RemoteSSH $CurrentEnv $startCmd

    Write-Host ">>> [3/3] 正在驗證服務就緒狀態 (最多等待 30 秒)..." -ForegroundColor Yellow
    $startTime = Get-Date
    $isUp = $false
    while (((Get-Date) - $startTime).TotalSeconds -lt 30) {
        Start-Sleep -Seconds 2
        $httpCode = (Invoke-RemoteSSH $CurrentEnv "curl -s -o /dev/null -w '%{http_code}' http://localhost:9095/carbon/login" -Silent $true)
        if ($httpCode) { $httpCode = $httpCode.Trim() }
        if ($httpCode -eq "200" -or $httpCode -eq "302") {
            $isUp = $true
            break
        }
        Write-Host -NoNewline "." -ForegroundColor Gray
    }
    Write-Host ""
    if ($isUp) {
        Write-Host "✔ 服務重啟成功！HTTP 響應碼: $httpCode" -ForegroundColor Green
    } else {
        Write-Host "⚠️ 等待超時或服務尚未完全就緒。" -ForegroundColor Yellow
    }
    exit 0
}

# ==================== 互動選單 (無特定參數時) ====================
$ActionChoice = 1
if (-not $SkipBuild -and -not $Force) {
    Write-Host "請選擇部署操作 (5 秒後將預設執行 [1]):" -ForegroundColor White
    Write-Host "  [1] 完整部署 (Maven 打包 + 上傳 + 重啟 + 健康檢查) [預設]" -ForegroundColor Green
    Write-Host "  [2] 快速部署 (略過打包，直接上傳現有 Jar + 重啟)" -ForegroundColor Cyan
    Write-Host "  [3] 僅檢查服務狀態" -ForegroundColor Gray
    Write-Host "  [4] 僅重啟服務" -ForegroundColor Gray
    Write-Host "  [5] 查看最新日誌" -ForegroundColor Gray
    Write-Host ""

    $timeoutSeconds = 5
    $keyAvailable = $false
    Write-Host -NoNewline "倒數 $timeoutSeconds 秒 (按 1~5 選擇，或按 Enter 直接執行)... " -ForegroundColor Yellow

    for ($i = $timeoutSeconds; $i -gt 0; $i--) {
        if ([Console]::KeyAvailable) {
            $keyAvailable = $true
            break
        }
        Start-Sleep -Milliseconds 1000
        Write-Host -NoNewline "$i " -ForegroundColor Yellow
    }
    Write-Host ""

    if ($keyAvailable) {
        $readKey = [Console]::ReadKey($true).KeyChar
        if ($readKey -eq '2') {
            $ActionChoice = 2
        } elseif ($readKey -eq '3') {
            & $PSCommandPath -Env $SelectedEnvKey -OnlyCheck
            exit 0
        } elseif ($readKey -eq '4') {
            & $PSCommandPath -Env $SelectedEnvKey -RestartOnly
            exit 0
        } elseif ($readKey -eq '5') {
            & $PSCommandPath -Env $SelectedEnvKey -TailLog
            exit 0
        } else {
            $ActionChoice = 1
        }
    } else {
        Write-Host "已超時，自動開始執行 [1] 完整部署..." -ForegroundColor Green
        $ActionChoice = 1
    }
} elseif ($SkipBuild) {
    $ActionChoice = 2
}

# ==================== 步驟 1: Maven 打包 ====================
if ($ActionChoice -eq 1) {
    Write-Host "`n>>> [步驟 1/5] 執行本地 Maven 打包編譯 (mvn package -DskipTests)..." -ForegroundColor Yellow
    
    # 檢查並自動釋放本地運行中的 jar 佔用
    $lockingProcs = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -match "carbon-0.0.1-SNAPSHOT\.jar" }
    if ($lockingProcs) {
        Write-Host "  偵測到本機有正在運行的背景服務佔用 Jar 檔案，正在自動停止以釋放鎖定..." -ForegroundColor Yellow
        foreach ($lp in $lockingProcs) {
            Stop-Process -Id $lp.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Milliseconds 500
    }
    
    Push-Location $CarbonDir
    try {
        & mvn package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "`n[錯誤] Maven 打包失敗！請確認程式碼無語法錯誤後再重試。" -ForegroundColor Red
            Pop-Location
            exit 1
        }
    } finally {
        Pop-Location
    }
    Write-Host "✔ Maven 打包成功！" -ForegroundColor Green
} else {
    Write-Host "`n>>> [步驟 1/5] 略過 Maven 打包，使用既有 Jar 檔。" -ForegroundColor Gray
}

# 檢查本機 Jar 是否存在
if (-not (Test-Path $LocalJar)) {
    Write-Host "[錯誤] 找不到本地 Jar 檔: $LocalJar！請先執行 Maven 打包。" -ForegroundColor Red
    exit 1
}

$localJarInfo = Get-Item $LocalJar
$jarSizeMB = [math]::Round($localJarInfo.Length / 1MB, 2)
$jarTime = $localJarInfo.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")
Write-Host "  本地 Jar: $LocalJar ($jarSizeMB MB, 更新時間: $jarTime)" -ForegroundColor Gray

# ==================== 步驟 2: 遠端目錄準備與既有版本備份 ====================
Write-Host "`n>>> [步驟 2/5] 正在準備遠端目錄與備份舊版 Jar..." -ForegroundColor Yellow
$backupCmd = "mkdir -p $($CurrentEnv.RemoteDir)/backup $($CurrentEnv.RemoteDir)/logs && cd $($CurrentEnv.RemoteDir) && if [ -f carbon-0.0.1-SNAPSHOT.jar ]; then cp carbon-0.0.1-SNAPSHOT.jar backup/carbon-0.0.1-SNAPSHOT_`$(date +%Y%m%d_%H%M%S).jar && ls -t backup/carbon-0.0.1-SNAPSHOT_*.jar 2>/dev/null | sed -e '1,5d' | xargs -d '\n' rm -f 2>/dev/null; echo '遠端備份完成 (保留最新 5 份備份)'; fi"
Invoke-RemoteSSH $CurrentEnv $backupCmd

# ==================== 步驟 3: 上傳 Jar 檔 ====================
Write-Host "`n>>> [步驟 3/5] 正在上傳 Jar 檔至遠端伺服器 (經由 PSCP)..." -ForegroundColor Yellow
$tempRemoteJar = "$($CurrentEnv.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar.tmp"

$uploadCode = Invoke-RemoteSCP $CurrentEnv $LocalJar $tempRemoteJar
if ($uploadCode -ne 0) {
    Write-Host "`n[錯誤] PSCP 上傳失敗！請檢查網路連線或金鑰權限。" -ForegroundColor Red
    exit 1
}
Write-Host "✔ 上傳成功！" -ForegroundColor Green

# 原子性替換遠端檔案
Write-Host "  正在更新遠端檔案指向..." -ForegroundColor Gray
Invoke-RemoteSSH $CurrentEnv "mv -f $tempRemoteJar $($CurrentEnv.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar && chmod 755 $($CurrentEnv.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar"

# ==================== 步驟 4: 停止舊進程並重啟服務 ====================
Write-Host "`n>>> [步驟 4/5] 正在重啟遠端服務..." -ForegroundColor Yellow
$restartCmd = "pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true; sleep 2; pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || sudo pkill -9 -f '\[c\]arbon-0.0.1-SNAPSHOT.jar' || true; cd $($CurrentEnv.RemoteDir) && nohup java -Duser.timezone=Asia/Taipei -jar carbon-0.0.1-SNAPSHOT.jar --spring.profiles.active=$($CurrentEnv.Profile) > logs/console.log 2>&1 &"
Invoke-RemoteSSH $CurrentEnv $restartCmd

# ==================== 步驟 5: 驗證服務健康狀態 ====================
Write-Host "`n>>> [步驟 5/5] 等待服務啟動並驗證 HTTP 登入頁狀態 (最多 30 秒)..." -ForegroundColor Yellow
$startTime = Get-Date
$isUp = $false
while (((Get-Date) - $startTime).TotalSeconds -lt 30) {
    Start-Sleep -Seconds 2
    $httpCode = (Invoke-RemoteSSH $CurrentEnv "curl -s -o /dev/null -w '%{http_code}' http://localhost:9095/carbon/login" -Silent $true)
    if ($httpCode) { $httpCode = $httpCode.Trim() }
    if ($httpCode -eq "200" -or $httpCode -eq "302") {
        $isUp = $true
        break
    }
    Write-Host -NoNewline "." -ForegroundColor Gray
}
Write-Host ""

if ($isUp) {
    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "      🎉 部署成功！碳權交易平台服務已正常運行 (HTTP $httpCode)       " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
} else {
    Write-Host "`n============================================================" -ForegroundColor Red
    Write-Host "      ⚠️ 注意：服務未能在 30 秒內響應 HTTP 200，請查看日誌        " -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
}

Write-Host "`n[遠端進程與通訊埠狀態]" -ForegroundColor Cyan
$chkProcCmd = "if pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' > /dev/null 2>&1; then echo '【運行中】 PID: '`$(pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' | xargs); netstat -tuln 2>/dev/null | grep 9095 || ss -tuln 2>/dev/null | grep 9095 || true; else echo '【未運行】'; fi"
Invoke-RemoteSSH $CurrentEnv $chkProcCmd

Write-Host "`n[最新 15 筆運作日誌]" -ForegroundColor Cyan
Invoke-RemoteSSH $CurrentEnv "tail -n 15 $($CurrentEnv.RemoteDir)/logs/carbon.log"

Write-Host "`n============================================================" -ForegroundColor Yellow
Write-Host "  外部存取網址: $($CurrentEnv.WebUrl)" -ForegroundColor Cyan
Write-Host "  部署作業全部完成！" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Yellow
