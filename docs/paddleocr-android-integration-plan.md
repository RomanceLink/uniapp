# PaddleOCR Android Integration Plan

## 目标

把后续训练好的 `led_digits` 和 `water_meter_digits` 模型接入当前 `phiron-ocr` 插件。

## 建议接入方式

1. 继续保留当前 `ML Kit` 方案作为兜底
2. 新增 `PaddleOCR` 推理实现
3. 专用接口优先走 PaddleOCR
4. 识别失败时再回退到当前规则 OCR

## 推荐目录规划

- `phiron-ocr/src/main/java/libs/`
  放 Paddle Lite / PaddleOCR Android 依赖
- `phiron-ocr/src/main/assets/models/led_digits/`
  放 LED 模型
- `phiron-ocr/src/main/assets/models/water_meter_digits/`
  放水表模型

## 代码拆分建议

- `PaddleOcrEngine.kt`
  负责模型加载和推理
- `LedDisplayRecognizer.kt`
  负责 LED 场景
- `WaterMeterRecognizer.kt`
  负责水表场景

## 接口兼容策略

保持当前对外接口不变：

- `recognizeLedDisplay`
- `recognizeWaterMeter`

这样后面替换推理底座时，前端不用改。

## 推进顺序

1. 先准备好样本和标注
2. 完成模型训练和导出
3. 把模型文件放到 assets
4. 再接 Android 推理
