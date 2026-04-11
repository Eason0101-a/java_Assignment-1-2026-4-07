# Vanishing Line 身高估測之實作與分析：半自動點選版本

Implementation and Analysis of Vanishing Line Height Estimation (Semi-Automatic)

## 題目目標

本作業目標為：

1. 以單張照片中的透視資訊（vanishing line）建立相對比例模型。
2. 以已知身高基準同學（180 cm）推估同張照片中其他同學身高。
3. 以半自動方式（滑鼠點選）降低手動輸入座標負擔。
4. 對主要流程進行時間複雜度分析，驗證方法可行性。

## 我設計的方法與資料結構

本專案以 Java 實作半自動估測流程，核心資料結構如下：

1. PixelPoint
- 代表像素座標點 `(x, y)`。

2. Person
- 代表人物資料，包含 `name`、`head`（頭頂點）、`foot`（腳底點）。

3. ClickPurpose
- 定義點選順序：
  - `HORIZON_A`、`HORIZON_B`
  - `REF_HEAD`、`REF_FOOT`
  - `TARGET_HEAD`、`TARGET_FOOT`

4. ClickCollector 與 ImagePanel
- `ClickCollector`：負責建立視窗、收集滑鼠點選、同步等待點選完成。
- `ImagePanel`：負責顯示圖片、畫出已點選點與地平線參考線。

## 操作流程（高階）

1. 初始化程式，輸入照片數量。
2. 每張照片輸入：圖片路徑、目標同學數量、各目標姓名。
3. 開啟圖片視窗並依序點選：
	- Horizon 點 A
	- Horizon 點 B
	- 基準同學頭頂與腳底
	- 各目標同學頭頂與腳底
4. 計算每位人物的投影比例：
	- `pixelHeight = |y_foot - y_head|`
	- `horizonY(x_foot)`
	- `ratio = pixelHeight / |y_foot - horizonY(x_foot)|`
5. 以基準同學換算目標身高：
	- `H_target = 180 * (ratio_target / ratio_ref)`
6. 輸出每位目標同學估測結果（cm）。

## 時間複雜度分析

令：
- `n` = 單張照片目標同學人數
- `k` = 單張照片總點選數（`k = 4 + 2n`）

主要步驟複雜度：

1. 點選資料收集
- 使用者每點一次記錄一次，時間 `O(k)`。

2. 每位人物估測
- `projectedHeight()` 與 `yOnLine()` 皆為常數時間，單人 `O(1)`。
- `n` 位目標總計 `O(n)`。

3. 單張照片整體
- 讀圖與繪圖屬影像處理成本；就演算法估測主流程為 `O(k + n)`。
- 由於 `k = 4 + 2n`，可視為線性時間 `O(n)`。

空間複雜度：
- 儲存點位與人物資料為 `O(k + n)`，整體為 `O(n)`。

## 程式檔案

1. `SemiAutoHeightEstimator.java`
- 主程式，含 UI 點選、資料收集與身高估測邏輯。

2. `pic1 (1).jpg`、`pic3.jpg`
- 測試用照片。

3. `執行結果.png`
- 執行畫面或結果截圖。

## 執行方式

在專案資料夾開啟終端機後執行：

```bash
javac SemiAutoHeightEstimator.java
java SemiAutoHeightEstimator
```

## 實測結果（範例輸出）

以下為主控台輸出格式範例：

```text
=== 半自動 Vanishing Line 身高估測工具 ===
流程: 每張圖用滑鼠點選關鍵點，基準同學固定 180 cm

請輸入照片數量 (例如 4): 1

--- 照片 1 ---
請輸入圖片檔案路徑: pic3.jpg
此照片要測量幾位目標同學: 2
目標同學 1 名稱: A
目標同學 2 名稱: B

照片結果: pic3.jpg
  A                    -> 176.41 cm
  B                    -> 183.39 cm

全部完成。
```

## 準確度建議

1. 頭頂點應選最高可見點，腳底點應選接地點。
2. Horizon A/B 建議拉開距離，避免線段過短造成不穩定。
3. 同張照片所有人物應使用同一條 vanishing line。
4. 若腳點接近 horizon，分母接近 0，誤差會明顯放大。

## 結論

本實作以半自動點選方式完成單張照片多人身高估測，兼顧操作性與可解釋性。透過基準同學（180 cm）與透視比例轉換，可在不需相機內參的情況下進行相對身高推估。演算法主流程在每張照片上為線性時間，適合課堂作業與中小型資料量的實驗分析。
