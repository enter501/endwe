# Endwe

以 Kotlin、Jetpack Compose 與 MVVM 開發的氣候監控 Android App。

## 已完成的功能

- 高雄、台南、台北快速切換
- 即時溫度、體感溫度、濕度、降雨量與風速
- 未來 12 小時溫度與降雨機率
- 未來 7 日高低溫、天氣與降雨機率
- 高溫、紫外線、降雨與強風提醒
- 下拉更新、右上角更新按鈕、錯誤重試
- 深色氣候儀表板介面

## 技術規格

- Kotlin + Jetpack Compose
- MVVM + StateFlow + Coroutines
- minSdk 26、targetSdk 35、JDK 17
- 資料來源：[Open-Meteo Weather Forecast API](https://open-meteo.com/en/docs)
- App 不需要 API Key

## 建置方式

在 Android Studio 開啟本目錄，等待 Gradle Sync 完成後直接執行 App。

完整測試與建置可在 PowerShell 執行隨附腳本：

```powershell
.\build.ps1
```

如果只需要產生 APK，可直接執行：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 會產生在：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 正式 Release APK

固定簽章金鑰與密碼設定存放在專案外：

```text
D:\AI\cos\Signing\Endwe\endwe-release.jks
D:\AI\cos\Signing\Endwe\keystore.properties
```

請完整備份 `D:\AI\cos\Signing\Endwe`；後續版本必須使用相同金鑰才能覆蓋安裝及更新。

執行正式測試與建置：

```powershell
.\build-release.ps1
```

正式 APK 會產生在：

```text
app\build\outputs\apk\release\Endwe-v1.0.0-release.apk
```
