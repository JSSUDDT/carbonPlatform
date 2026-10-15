# ===================================================================
# 碳權交易平台 - 遠端一鍵部署圖形化工具 (PowerShell GUI)
# 支援環境: 測試環境 (SIT) / 正式環境 (PROD)
# ===================================================================
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

[System.Windows.Forms.Application]::EnableVisualStyles()

try {
    # ==================== 環境配置參數 ====================
    $Environments = @{
        "sit" = @{
            Id          = "sit"
            Name        = "測試環境 (SIT)"
            Host        = "61.91.11.227"
            Port        = "4936"
            User        = "etreego"
            Pass        = 'CosQQ$154as'
            Key         = $null
            AuthType    = "密碼認證"
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
            AuthType    = "PPK 金鑰認證"
            RemoteDir   = "/opt/sit/carbonPlatform"
            Profile     = "prod"
            SyncNote    = "開啟 (定時排程同步拉取外部資料庫)"
            WebUrl      = "http://52.185.184.91:9095/carbon/login"
            IsProd      = $true
        }
    }

    # 本地路徑偵測
    $CurrentDir = $PSScriptRoot
    if (Test-Path (Join-Path $CurrentDir "carbon\pom.xml")) {
        $CarbonDir = Join-Path $CurrentDir "carbon"
    } elseif (Test-Path (Join-Path $CurrentDir "pom.xml")) {
        $CarbonDir = $CurrentDir
    } else {
        $CarbonDir = "d:\vsCodeProjects\carbon_0512\carbon"
    }

    $LocalJar = Join-Path $CarbonDir "target\carbon-0.0.1-SNAPSHOT.jar"

    # 工具尋找輔助函式
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
        [System.Windows.Forms.MessageBox]::Show("找不到 plink.exe 或 pscp.exe！請確認已安裝 PuTTY (預設安裝於 C:\Program Files\PuTTY)。", "缺少必要工具", [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Error)
        exit 1
    }

    # ==================== 建立主視窗 ====================
    $form = New-Object System.Windows.Forms.Form
    $form.Text = "碳權交易整合平台 - 遠端伺服器一鍵部署中心 (SIT / PROD)"
    $form.Size = New-Object System.Drawing.Size(940, 720)
    $form.StartPosition = "CenterScreen"
    $form.BackColor = [System.Drawing.Color]::FromArgb(245, 247, 250)
    $form.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 9.5)

    # 頂部資訊列 (高雅深海藍)
    $panelTop = New-Object System.Windows.Forms.Panel
    $panelTop.Dock = "Top"
    $panelTop.Height = 118
    $panelTop.BackColor = [System.Drawing.Color]::FromArgb(20, 35, 60)
    $form.Controls.Add($panelTop)

    # 標題
    $lblTitle = New-Object System.Windows.Forms.Label
    $lblTitle.Text = "🌿 碳權交易整合平台 - 遠端自動化發布系統"
    $lblTitle.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 12.5, [System.Drawing.FontStyle]::Bold)
    $lblTitle.ForeColor = [System.Drawing.Color]::White
    $lblTitle.Location = New-Object System.Drawing.Point(18, 10)
    $lblTitle.AutoSize = $true
    $panelTop.Controls.Add($lblTitle)

    # 環境選擇標籤
    $lblEnvTag = New-Object System.Windows.Forms.Label
    $lblEnvTag.Text = "切換目標環境:"
    $lblEnvTag.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 9.5, [System.Drawing.FontStyle]::Bold)
    $lblEnvTag.ForeColor = [System.Drawing.Color]::FromArgb(220, 230, 245)
    $lblEnvTag.Location = New-Object System.Drawing.Point(20, 39)
    $lblEnvTag.AutoSize = $true
    $panelTop.Controls.Add($lblEnvTag)

    # 環境下拉選單
    $cmbEnv = New-Object System.Windows.Forms.ComboBox
    $cmbEnv.DropDownStyle = [System.Windows.Forms.ComboBoxStyle]::DropDownList
    $cmbEnv.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 9.5)
    $cmbEnv.Size = New-Object System.Drawing.Size(320, 28)
    $cmbEnv.Location = New-Object System.Drawing.Point(120, 36)
    $cmbEnv.Items.Add("🧪 測試環境 (SIT - 61.91.11.227:4936)") | Out-Null
    $cmbEnv.Items.Add("🚀 正式環境 (PROD - 52.185.184.91:22)") | Out-Null
    $cmbEnv.SelectedIndex = 0
    $panelTop.Controls.Add($cmbEnv)

    # 副資訊列 1 (主機、目錄)
    $lblSub1 = New-Object System.Windows.Forms.Label
    $lblSub1.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 8.8)
    $lblSub1.ForeColor = [System.Drawing.Color]::FromArgb(180, 205, 237)
    $lblSub1.Location = New-Object System.Drawing.Point(20, 68)
    $lblSub1.AutoSize = $true
    $panelTop.Controls.Add($lblSub1)

    # 副資訊列 2 (運行配置、排程、外部網址)
    $lblSub2 = New-Object System.Windows.Forms.Label
    $lblSub2.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 8.8)
    $lblSub2.ForeColor = [System.Drawing.Color]::FromArgb(140, 220, 240)
    $lblSub2.Location = New-Object System.Drawing.Point(20, 89)
    $lblSub2.AutoSize = $true
    $panelTop.Controls.Add($lblSub2)

    # 取得目前選擇的環境設定
    function Get-CurrentEnvConfig {
        if ($cmbEnv.SelectedIndex -eq 1) {
            return $Environments["prod"]
        } else {
            return $Environments["sit"]
        }
    }

    # 更新頂部資訊函式
    function Update-TopInfo {
        $envCfg = Get-CurrentEnvConfig
        $lblSub1.Text = "目標主機: $($envCfg.User)@$($envCfg.Host):$($envCfg.Port) ($($envCfg.AuthType))   |   遠端目錄: $($envCfg.RemoteDir)"
        $lblSub2.Text = "環境Profile: $($envCfg.Profile)   |   外部排程: $($envCfg.SyncNote)   |   網址: $($envCfg.WebUrl)"
    }
    Update-TopInfo

    # 環境切換事件
    $cmbEnv.Add_SelectedIndexChanged({
        Update-TopInfo
        $envCfg = Get-CurrentEnvConfig
        Append-Log "已切換目標環境為: $($envCfg.Name)" ([System.Drawing.Color]::Cyan)
    })

    # 底部狀態列
    $statusStrip = New-Object System.Windows.Forms.StatusStrip
    $statusLabel = New-Object System.Windows.Forms.ToolStripStatusLabel
    $statusLabel.Text = "系統就緒 (請點擊下方按鈕開始操作)"
    $statusStrip.Items.Add($statusLabel) | Out-Null
    $form.Controls.Add($statusStrip)

    # 底部按鈕區
    $panelBottom = New-Object System.Windows.Forms.Panel
    $panelBottom.Dock = "Bottom"
    $panelBottom.Height = 65
    $panelBottom.BackColor = [System.Drawing.Color]::FromArgb(235, 239, 245)
    $form.Controls.Add($panelBottom)

    # 中間日誌區
    $panelCenter = New-Object System.Windows.Forms.Panel
    $panelCenter.Dock = "Fill"
    $panelCenter.Padding = New-Object System.Windows.Forms.Padding(12, 10, 12, 5)
    $form.Controls.Add($panelCenter)

    $txtLog = New-Object System.Windows.Forms.RichTextBox
    $txtLog.Dock = "Fill"
    $txtLog.BackColor = [System.Drawing.Color]::FromArgb(15, 20, 28)
    $txtLog.ForeColor = [System.Drawing.Color]::FromArgb(130, 220, 130)
    $txtLog.Font = New-Object System.Drawing.Font("Consolas", 9.5)
    $txtLog.ReadOnly = $true
    $txtLog.ScrollBars = "Vertical"
    $panelCenter.Controls.Add($txtLog)

    # 輔助日誌輸出
    function Append-Log {
        param([string]$Text, [System.Drawing.Color]$Color)
        if (-not $Color) { $Color = [System.Drawing.Color]::FromArgb(180, 220, 180) }
        $txtLog.SelectionStart = $txtLog.TextLength
        $txtLog.SelectionLength = 0
        $txtLog.SelectionColor = $Color
        $timestamp = (Get-Date).ToString("HH:mm:ss")
        $txtLog.AppendText("[$timestamp] $Text`r`n")
        $txtLog.ScrollToCaret()
        [System.Windows.Forms.Application]::DoEvents()
    }

    # 封裝 SSH 參數產生函式
    function Get-SSHArgs {
        param([hashtable]$TargetEnv, [string]$Command)
        $args = @("-batch", "-P", $TargetEnv.Port, "-l", $TargetEnv.User)
        if ($TargetEnv.Key) {
            $args += @("-i", $TargetEnv.Key)
        } elseif ($TargetEnv.Pass) {
            $args += @("-pw", $TargetEnv.Pass)
        }
        $args += @($TargetEnv.Host, $Command)
        return $args
    }

    # 封裝 SCP 參數產生函式
    function Get-SCPArgs {
        param([hashtable]$TargetEnv, [string]$LocalFile, [string]$RemoteFile)
        $args = @("-batch", "-P", $TargetEnv.Port)
        if ($TargetEnv.Key) {
            $args += @("-i", $TargetEnv.Key)
        } elseif ($TargetEnv.Pass) {
            $args += @("-pw", $TargetEnv.Pass)
        }
        $args += @($LocalFile, "$($TargetEnv.User)@$($TargetEnv.Host):$RemoteFile")
        return $args
    }

    # 執行外部指令並即時串流輸出 (非阻塞安全讀取)
    function Exec-Streaming {
        param([string]$FilePath, [string[]]$ArgumentList, [string]$StepName, [string]$WorkingDir = $null)
        Append-Log "▶ 開始執行: $StepName" ([System.Drawing.Color]::Yellow)

        $formatted = @()
        foreach ($arg in $ArgumentList) {
            if ($arg -match '\s|[|><&]') {
                $formatted += '"' + $arg.Replace('"', '\"') + '"'
            } else {
                $formatted += $arg
            }
        }

        $pinfo = New-Object System.Diagnostics.ProcessStartInfo
        $pinfo.FileName = $FilePath
        $pinfo.Arguments = ($formatted -join " ")
        if ($WorkingDir) {
            $pinfo.WorkingDirectory = $WorkingDir
        }
        $pinfo.RedirectStandardOutput = $true
        $pinfo.RedirectStandardError = $true
        $pinfo.UseShellExecute = $false
        $pinfo.CreateNoWindow = $true
        $pinfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
        $pinfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8

        $p = New-Object System.Diagnostics.Process
        $p.StartInfo = $pinfo

        try {
            $p.Start() | Out-Null
        } catch {
            Append-Log "無法啟動程式: $_" ([System.Drawing.Color]::Red)
            return -1
        }

        while (-not $p.HasExited) {
            while ($p.StandardOutput.Peek() -ge 0) {
                $line = $p.StandardOutput.ReadLine()
                if ($line) { Append-Log $line ([System.Drawing.Color]::FromArgb(210, 210, 210)) }
            }
            while ($p.StandardError.Peek() -ge 0) {
                $errLine = $p.StandardError.ReadLine()
                if ($errLine) { Append-Log $errLine ([System.Drawing.Color]::FromArgb(255, 140, 140)) }
            }
            [System.Windows.Forms.Application]::DoEvents()
            Start-Sleep -Milliseconds 50
        }

        # 讀取剩餘輸出
        $restOut = $p.StandardOutput.ReadToEnd()
        if ($restOut) {
            $lines = $restOut.Split([string[]]@("`r`n", "`n"), [System.StringSplitOptions]::RemoveEmptyEntries)
            foreach ($l in $lines) {
                if ($l.Trim()) { Append-Log $l ([System.Drawing.Color]::FromArgb(210, 210, 210)) }
            }
        }
        $restErr = $p.StandardError.ReadToEnd()
        if ($restErr) {
            $linesErr = $restErr.Split([string[]]@("`r`n", "`n"), [System.StringSplitOptions]::RemoveEmptyEntries)
            foreach ($l in $linesErr) {
                if ($l.Trim()) { Append-Log $l ([System.Drawing.Color]::FromArgb(255, 140, 140)) }
            }
        }

        $exitCode = $p.ExitCode
        $p.Close()

        if ($exitCode -eq 0) {
            Append-Log "✔ $StepName 完成" ([System.Drawing.Color]::FromArgb(100, 255, 100))
        } else {
            Append-Log "❌ $StepName 結束 (代碼: $exitCode)" ([System.Drawing.Color]::FromArgb(255, 100, 100))
        }
        return $exitCode
    }

    # 控制按鈕啟用狀態
    function Set-ButtonsEnabled {
        param([bool]$Enabled)
        $btnFull.Enabled  = $Enabled
        $btnFast.Enabled  = $Enabled
        $btnCheck.Enabled = $Enabled
        $btnLogs.Enabled  = $Enabled
        $btnStop.Enabled  = $Enabled
        $cmbEnv.Enabled   = $Enabled
    }

    # 執行部署主程序
    function Start-Deployment {
        param([bool]$DoMavenBuild)
        $envCfg = Get-CurrentEnvConfig

        # 正式環境二次確認
        if ($envCfg.IsProd) {
            $actText = if ($DoMavenBuild) { "一鍵完整部署 (含Maven編譯)" } else { "快速部署 (現有Jar直傳)" }
            $confirmMsg = "⚠️【正式環境安全確認】`n`n目標伺服器：$($envCfg.Name)`n主機位址：$($envCfg.User)@$($envCfg.Host):$($envCfg.Port)`n操作項目：$actText`n`n此操作將會更新正式環境線上運行的碳權交易平台。`n`n確定要繼續執行嗎？"
            $dialogRes = [System.Windows.Forms.MessageBox]::Show($confirmMsg, "正式環境部署安全確認", [System.Windows.Forms.MessageBoxButtons]::YesNo, [System.Windows.Forms.MessageBoxIcon]::Warning)
            if ($dialogRes -ne [System.Windows.Forms.DialogResult]::Yes) {
                Append-Log "使用者已取消正式環境部署。" ([System.Drawing.Color]::Yellow)
                return
            }
        }

        Set-ButtonsEnabled $false

        try {
            $txtLog.Clear()
            Append-Log "==========================================================" ([System.Drawing.Color]::Cyan)
            Append-Log "🌿 開始執行碳權交易平台自動化部署流程" ([System.Drawing.Color]::Cyan)
            Append-Log "目標環境: $($envCfg.Name) ($($envCfg.User)@$($envCfg.Host):$($envCfg.Port))" ([System.Drawing.Color]::Cyan)
            Append-Log "Spring Profile: $($envCfg.Profile)   |   時區: Asia/Taipei" ([System.Drawing.Color]::Cyan)
            Append-Log "外部排程拉取: $($envCfg.SyncNote)" ([System.Drawing.Color]::Cyan)
            Append-Log "==========================================================" ([System.Drawing.Color]::Cyan)
            $statusLabel.Text = "部署進行中..."

            # 步驟 1: Maven 打包編譯
            if ($DoMavenBuild) {
                $statusLabel.Text = "步驟 1/5: 正在進行本地 Maven 打包編譯..."

                # 釋放本地佔用
                $lockingProcs = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -match "carbon-0.0.1-SNAPSHOT\.jar" }
                if ($lockingProcs) {
                    Append-Log "偵測到本機有正在運行的背景服務佔用 Jar 檔案，正在自動停止以釋放鎖定..." ([System.Drawing.Color]::Yellow)
                    foreach ($lp in $lockingProcs) {
                        Stop-Process -Id $lp.ProcessId -Force -ErrorAction SilentlyContinue
                    }
                    Start-Sleep -Milliseconds 500
                }

                $ret = Exec-Streaming "cmd.exe" @("/c", "mvn package -DskipTests") "本地 Maven 打包編譯" $CarbonDir
                if ($ret -ne 0) {
                    Append-Log "Maven 打包失敗，已中止部署。" ([System.Drawing.Color]::Red)
                    $statusLabel.Text = "Maven 編譯失敗"
                    return
                }
            } else {
                Append-Log ">>> 略過 Maven 打包，直接部署既有 Jar 檔。" ([System.Drawing.Color]::Gray)
            }

            # 檢查本地 Jar
            if (-not (Test-Path $LocalJar)) {
                Append-Log "錯誤: 找不到本地檔案 $LocalJar！請先執行完整編譯打包。" ([System.Drawing.Color]::Red)
                $statusLabel.Text = "找不到本地 Jar"
                return
            }
            $jarInfo = Get-Item $LocalJar
            $jarMB = [math]::Round($jarInfo.Length / 1MB, 2)
            Append-Log "準備發布本地檔案: $LocalJar ($jarMB MB, 更新於 $($jarInfo.LastWriteTime))" ([System.Drawing.Color]::White)

            # 步驟 2: 備份遠端檔案
            $statusLabel.Text = "步驟 2/5: 正在準備遠端目錄與備份舊版 Jar..."
            $bkCmd = "mkdir -p $($envCfg.RemoteDir)/backup $($envCfg.RemoteDir)/logs && cd $($envCfg.RemoteDir) && if [ -f carbon-0.0.1-SNAPSHOT.jar ]; then cp carbon-0.0.1-SNAPSHOT.jar backup/carbon-0.0.1-SNAPSHOT_`$(date +%Y%m%d_%H%M%S).jar && ls -t backup/carbon-0.0.1-SNAPSHOT_*.jar 2>/dev/null | sed -e '1,5d' | xargs -d '\n' rm -f 2>/dev/null; echo '遠端備份完成 (保留最新 5 份備份)'; fi"
            $sshBkArgs = Get-SSHArgs $envCfg $bkCmd
            Exec-Streaming $Plink $sshBkArgs "遠端目錄建立與舊版備份"

            # 步驟 3: PSCP 上傳
            $statusLabel.Text = "步驟 3/5: 正在透過 PSCP 安全上傳最新 Jar 檔..."
            $tempRemoteJar = "$($envCfg.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar.tmp"
            $scpArgs = Get-SCPArgs $envCfg $LocalJar $tempRemoteJar
            $ret = Exec-Streaming $Pscp $scpArgs "上傳 Jar 至遠端伺服器"
            if ($ret -ne 0) {
                Append-Log "上傳失敗，已中止部署。" ([System.Drawing.Color]::Red)
                $statusLabel.Text = "PSCP 上傳失敗"
                return
            }

            # 原子性替換遠端檔案
            Append-Log "正在更新遠端檔案指向..." ([System.Drawing.Color]::Gray)
            $mvCmd = "mv -f $tempRemoteJar $($envCfg.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar && chmod 755 $($envCfg.RemoteDir)/carbon-0.0.1-SNAPSHOT.jar"
            $sshMvArgs = Get-SSHArgs $envCfg $mvCmd
            Exec-Streaming $Plink $sshMvArgs "替換主程式檔案"

            # 步驟 4: 停止舊進程並重啟服務
            $statusLabel.Text = "步驟 4/5: 正在重啟遠端服務..."
            $restartCmd = "pkill -f 'carbon-0.0.1-SNAPSHOT.jar' || sudo pkill -f 'carbon-0.0.1-SNAPSHOT.jar' || true; sleep 2; pkill -9 -f 'carbon-0.0.1-SNAPSHOT.jar' || sudo pkill -9 -f 'carbon-0.0.1-SNAPSHOT.jar' || true; cd $($envCfg.RemoteDir) && nohup java -Duser.timezone=Asia/Taipei -jar carbon-0.0.1-SNAPSHOT.jar --spring.profiles.active=$($envCfg.Profile) > logs/console.log 2>&1 &"
            $sshRestartArgs = Get-SSHArgs $envCfg $restartCmd
            Exec-Streaming $Plink $sshRestartArgs "停止舊服務並背景啟動新服務"

            # 步驟 5: 健康檢查 (輪詢 curl 登入頁)
            $statusLabel.Text = "步驟 5/5: 等待服務啟動並檢驗 HTTP 登入頁狀態 (最多 30 秒)..."
            Append-Log "正在輪詢檢驗服務健康狀態 (最多 30 秒)..." ([System.Drawing.Color]::Yellow)

            $startTime = Get-Date
            $isUp = $false
            $httpCode = ""
            while (((Get-Date) - $startTime).TotalSeconds -lt 30) {
                Start-Sleep -Seconds 2
                $curlCmd = "curl -s -o /dev/null -w '%{http_code}' http://localhost:9095/carbon/login"
                $sshCurlArgs = Get-SSHArgs $envCfg $curlCmd
                $checkProc = New-Object System.Diagnostics.Process
                $checkProc.StartInfo.FileName = $Plink
                $checkProc.StartInfo.Arguments = ($sshCurlArgs -join " ")
                $checkProc.StartInfo.RedirectStandardOutput = $true
                $checkProc.StartInfo.UseShellExecute = $false
                $checkProc.StartInfo.CreateNoWindow = $true
                $checkProc.Start() | Out-Null
                $httpCode = $checkProc.StandardOutput.ReadToEnd().Trim()
                $checkProc.WaitForExit(3000)

                if ($httpCode -eq "200" -or $httpCode -eq "302") {
                    $isUp = $true
                    break
                }
                Append-Log "  服務啟動中，等待響應... (目前: $httpCode)" ([System.Drawing.Color]::Gray)
                [System.Windows.Forms.Application]::DoEvents()
            }

            if ($isUp) {
                Append-Log "==========================================================" ([System.Drawing.Color]::FromArgb(100, 255, 100))
                Append-Log "🎉 恭喜！遠端自動發布與服務重啟已順利完成！" ([System.Drawing.Color]::FromArgb(100, 255, 100))
                Append-Log "HTTP 響應碼: $httpCode (服務正常運作)" ([System.Drawing.Color]::FromArgb(100, 255, 100))
                Append-Log "外部登入網址: $($envCfg.WebUrl)" ([System.Drawing.Color]::Cyan)
                Append-Log "==========================================================" ([System.Drawing.Color]::FromArgb(100, 255, 100))
                $statusLabel.Text = "部署成功 (HTTP $httpCode)"
            } else {
                Append-Log "⚠️ 警告：服務未能在 30 秒內響應 HTTP 200，可能仍在啟動中或發生異常。" ([System.Drawing.Color]::Red)
                $statusLabel.Text = "啟動等待超時"
            }

            # 顯示進程與通訊埠狀態
            $statCmd = "if pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' > /dev/null 2>&1; then echo '【運行中】 PID: '`$(pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' | xargs); netstat -tuln 2>/dev/null | grep 9095 || ss -tuln 2>/dev/null | grep 9095 || true; else echo '【未運行】'; fi"
            $sshStatArgs = Get-SSHArgs $envCfg $statCmd
            Exec-Streaming $Plink $sshStatArgs "確認進程與通訊埠"

            # 顯示最新日誌
            $tailCmd = "tail -n 15 $($envCfg.RemoteDir)/logs/carbon.log"
            $sshTailArgs = Get-SSHArgs $envCfg $tailCmd
            Exec-Streaming $Plink $sshTailArgs "最新 15 筆運作日誌"

        } catch {
            Append-Log "發生未預期異常: $_" ([System.Drawing.Color]::Red)
            $statusLabel.Text = "發生異常"
        } finally {
            Set-ButtonsEnabled $true
        }
    }

    # ==================== 按鈕配置 ====================
    # 按鈕 1: 一鍵完整部署 (含編譯)
    $btnFull = New-Object System.Windows.Forms.Button
    $btnFull.Text = "🚀 一鍵完整部署 (含編譯)"
    $btnFull.Size = New-Object System.Drawing.Size(185, 40)
    $btnFull.Location = New-Object System.Drawing.Point(15, 12)
    $btnFull.BackColor = [System.Drawing.Color]::FromArgb(40, 167, 69)
    $btnFull.ForeColor = [System.Drawing.Color]::White
    $btnFull.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnFull.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 9.5, [System.Drawing.FontStyle]::Bold)
    $btnFull.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnFull.Add_Click({ Start-Deployment $true })
    $panelBottom.Controls.Add($btnFull)

    # 按鈕 2: 快速部署 (現有Jar直傳)
    $btnFast = New-Object System.Windows.Forms.Button
    $btnFast.Text = "⚡ 快速部署 (現有Jar直傳)"
    $btnFast.Size = New-Object System.Drawing.Size(175, 40)
    $btnFast.Location = New-Object System.Drawing.Point(208, 12)
    $btnFast.BackColor = [System.Drawing.Color]::FromArgb(23, 162, 184)
    $btnFast.ForeColor = [System.Drawing.Color]::White
    $btnFast.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnFast.Font = New-Object System.Drawing.Font("Microsoft JhengHei UI", 9.5, [System.Drawing.FontStyle]::Bold)
    $btnFast.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnFast.Add_Click({ Start-Deployment $false })
    $panelBottom.Controls.Add($btnFast)

    # 按鈕 3: 檢查服務狀態
    $btnCheck = New-Object System.Windows.Forms.Button
    $btnCheck.Text = "🔍 檢查服務狀態"
    $btnCheck.Size = New-Object System.Drawing.Size(125, 40)
    $btnCheck.Location = New-Object System.Drawing.Point(391, 12)
    $btnCheck.BackColor = [System.Drawing.Color]::White
    $btnCheck.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnCheck.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnCheck.Add_Click({
        $envCfg = Get-CurrentEnvConfig
        try {
            Set-ButtonsEnabled $false
            $txtLog.Clear()
            $statusLabel.Text = "正在查詢 $($envCfg.Name) 服務狀態..."
            Append-Log "正在查詢 $($envCfg.Name) ($($envCfg.Host)) 服務狀態..." ([System.Drawing.Color]::Yellow)
            $chkCmd = "if pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' > /dev/null 2>&1; then echo '【運行中】 PID: '`$(pgrep -f 'carbon-0.0.1-SNAPSHOT.jar' | xargs); netstat -tuln 2>/dev/null | grep 9095 || ss -tuln 2>/dev/null | grep 9095 || true; else echo '【未運行】 目前沒有 Carbon 服務在運行'; fi"
            $args = Get-SSHArgs $envCfg $chkCmd
            Exec-Streaming $Plink $args "查詢服務狀態"
            $statusLabel.Text = "狀態查詢完畢"
        } catch {
            Append-Log "查詢時發生錯誤: $_" ([System.Drawing.Color]::Red)
        } finally {
            Set-ButtonsEnabled $true
        }
    })
    $panelBottom.Controls.Add($btnCheck)

    # 按鈕 4: 最新日誌
    $btnLogs = New-Object System.Windows.Forms.Button
    $btnLogs.Text = "📜 最新日誌"
    $btnLogs.Size = New-Object System.Drawing.Size(110, 40)
    $btnLogs.Location = New-Object System.Drawing.Point(524, 12)
    $btnLogs.BackColor = [System.Drawing.Color]::White
    $btnLogs.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnLogs.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnLogs.Add_Click({
        $envCfg = Get-CurrentEnvConfig
        try {
            Set-ButtonsEnabled $false
            $txtLog.Clear()
            $statusLabel.Text = "正在取得 $($envCfg.Name) 日誌..."
            Append-Log "正在抓取 $($envCfg.Name) 最新 30 行運作日誌..." ([System.Drawing.Color]::Yellow)
            $tailCmd = "tail -n 30 $($envCfg.RemoteDir)/logs/carbon.log"
            $args = Get-SSHArgs $envCfg $tailCmd
            Exec-Streaming $Plink $args "抓取最新日誌"
            $statusLabel.Text = "日誌抓取完成"
        } catch {
            Append-Log "抓取日誌時發生錯誤: $_" ([System.Drawing.Color]::Red)
        } finally {
            Set-ButtonsEnabled $true
        }
    })
    $panelBottom.Controls.Add($btnLogs)

    # 按鈕 5: 停止服務
    $btnStop = New-Object System.Windows.Forms.Button
    $btnStop.Text = "🛑 停止服務"
    $btnStop.Size = New-Object System.Drawing.Size(110, 40)
    $btnStop.Location = New-Object System.Drawing.Point(642, 12)
    $btnStop.BackColor = [System.Drawing.Color]::White
    $btnStop.ForeColor = [System.Drawing.Color]::FromArgb(220, 53, 69)
    $btnStop.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnStop.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnStop.Add_Click({
        $envCfg = Get-CurrentEnvConfig
        $dialogRes = [System.Windows.Forms.MessageBox]::Show("確定要停止 $($envCfg.Name) 的碳權服務嗎？", "停止服務確認", [System.Windows.Forms.MessageBoxButtons]::YesNo, [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($dialogRes -ne [System.Windows.Forms.DialogResult]::Yes) { return }

        try {
            Set-ButtonsEnabled $false
            $txtLog.Clear()
            $statusLabel.Text = "正在停止服務..."
            Append-Log "正在停止 $($envCfg.Name) 上的 Carbon 服務..." ([System.Drawing.Color]::Yellow)
            $stopCmd = "pkill -f 'carbon-0.0.1-SNAPSHOT.jar' || sudo pkill -f 'carbon-0.0.1-SNAPSHOT.jar' || true; sleep 2; pkill -9 -f 'carbon-0.0.1-SNAPSHOT.jar' || sudo pkill -9 -f 'carbon-0.0.1-SNAPSHOT.jar' || true; echo '服務已停止'"
            $args = Get-SSHArgs $envCfg $stopCmd
            Exec-Streaming $Plink $args "停止遠端服務"
            $statusLabel.Text = "服務已停止"
        } catch {
            Append-Log "停止服務時發生錯誤: $_" ([System.Drawing.Color]::Red)
        } finally {
            Set-ButtonsEnabled $true
        }
    })
    $panelBottom.Controls.Add($btnStop)

    # 按鈕 6: 清空日誌
    $btnClear = New-Object System.Windows.Forms.Button
    $btnClear.Text = "🧹 清空日誌"
    $btnClear.Size = New-Object System.Drawing.Size(95, 40)
    $btnClear.Location = New-Object System.Drawing.Point(805, 12)
    $btnClear.BackColor = [System.Drawing.Color]::White
    $btnClear.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
    $btnClear.Cursor = [System.Windows.Forms.Cursors]::Hand
    $btnClear.Add_Click({ $txtLog.Clear() })
    $panelBottom.Controls.Add($btnClear)

    # 初始化日誌訊息
    Append-Log "🌿 碳權交易平台遠端自動化發布工具已就緒。" ([System.Drawing.Color]::LightGray)
    Append-Log "本地專案路徑: $CarbonDir" ([System.Drawing.Color]::LightGray)
    Append-Log "目前選定環境: $($Environments['sit'].Name) ($($Environments['sit'].Host):$($Environments['sit'].Port))" ([System.Drawing.Color]::LightGray)
    Append-Log "外部登入網址: $($Environments['sit'].WebUrl)" ([System.Drawing.Color]::Cyan)
    Append-Log "可於上方切換目標環境 (SIT / PROD)，點擊下方按鈕即可開始執行作業。" ([System.Drawing.Color]::Yellow)

    # 顯示主視窗
    $form.ShowDialog() | Out-Null

} catch {
    [System.Windows.Forms.MessageBox]::Show("部署工具運行發生未預期異常:`n" + $_.Exception.ToString(), "執行錯誤", [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Error)
}
