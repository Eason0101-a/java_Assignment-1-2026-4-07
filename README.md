# Vanishing Line 身高測量（Java）

本作業以「穿 Just do it 衣服、身高 180cm」同學為基準，利用 vanishing line（地平線）與平行線透視概念，估算同張照片中其他同學身高。

## 1. 方法說明

對同一張照片中的每位同學，定義：

- `pixelHeight = |y_foot - y_head|`（人物在影像中的像素高度）
- `horizonY(x_foot)`（vanishing line 在該人物腳點 x 位置的 y 值）
- `footToHorizon = |y_foot - horizonY(x_foot)|`

投影比例量：

`ratio = pixelHeight / footToHorizon`

以基準同學（180cm）換算目標同學：

`H_target = 180 * (ratio_target / ratio_ref)`

## 2. 檔案

- `HeightEstimator.java`：主程式（純命令列輸入座標）
- `SemiAutoHeightEstimator.java`：半自動版本（開圖後滑鼠點選座標）
- `pic1 (1).jpg`、`pic3.jpg`：目前資料夾內的照片

## 3. 執行方式

### A. 純命令列版本

在本資料夾開啟終端機後執行：

```bash
javac HeightEstimator.java
java HeightEstimator
```

### B. 半自動滑鼠點選版本（建議）

```bash
javac SemiAutoHeightEstimator.java
java SemiAutoHeightEstimator
```

執行後會先輸入照片路徑與目標同學姓名，接著跳出圖片視窗，依序點選：

1. Horizon 點 A
2. Horizon 點 B
3. 基準同學頭頂（Just do it）
4. 基準同學腳底
5. 各目標同學頭頂與腳底（依提示順序）

點完後主控台會直接輸出該照片每位同學身高（cm）。

## 4. 輸入步驟（純命令列版，每張照片）

1. 輸入照片名稱
2. 輸入 vanishing line 上兩點座標（horizon point A / B）
3. 輸入基準同學（180cm）腳底與頭頂座標
4. 輸入要測量的目標同學數量
5. 逐位輸入目標同學名稱、腳底與頭頂座標

座標格式統一為：`x y`（例如 `512 938`）

## 5. 準確度建議

- 盡量精準點在頭頂最高點與鞋底接地點
- vanishing line 建議由場景中的平行地面線推估（如地磚線、走廊線）
- 同一張照片請使用同一條 vanishing line
- 若人物腳點非常接近 vanishing line，誤差會放大

## 6. 作業備註

你的題目提到「四張照片、其他四位同學」，目前資料夾中只有 2 張照片。程式可處理任意張數，你可直接輸入 `4` 並依序填入四張照片資料。若補上其餘照片，也可照同流程計算。
