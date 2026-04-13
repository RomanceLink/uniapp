# meter_ocr

这是给 `phiron-ocr` 后续做“小型专用模型”准备的训练目录。

当前建议做两条模型线：

- `led_digits`
  识别红色、绿色 LED 数码管，目标是电子称、控制器、仪表盘上的发光数字
- `water_meter_digits`
  识别液晶水表、黑色数字、小数点和末位小字

## 为什么现在这一步必须做

现有插件用的是通用 OCR + 规则增强：

- 对截图、裁剪后清晰数字还行
- 对手拍图，尤其是反光、模糊、透视、复杂背景，稳定性不够

所以继续调阈值的收益已经很低了。下一步正确方向是：

1. 先收集样本
2. 再做专用训练
3. 最后把模型接回 Android 插件

## 目录说明

- [images](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/images)
  放原始图片，建议按场景再分子目录
- [annotations](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/annotations)
  放标注文本
- [configs](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/configs)
  放训练配置说明
- [scripts](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/scripts)
  放准备数据的脚本
- [outputs](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/outputs)
  放训练输出或中间结果

## 最低样本建议

先别追求特别大，先做一个“小模型”验证：

- `led_digits`
  至少 200 张
- `water_meter_digits`
  至少 200 张

更稳的目标：

- 每类 500 到 1000 张

## 标注建议

推荐先做“整图到读数”的轻量方案：

- 一张图只标一个最终值
- 例如：
  - 电子称图：`0.23|989.4`
  - 水表图：`462796.7589`

其中：

- 电子称如果一张图里有两行有效数字，先用 `|` 分隔
- 水表一般只保留最终完整读数

样例见：
[labels.example.txt](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/annotations/labels.example.txt)

## 后续落地路线

建议按这个顺序推进：

1. 先收 50 张电子称 + 50 张水表，验证标注格式
2. 用 [split_by_scene.py](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/scripts/split_by_scene.py) 和 [prepare_dataset.py](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/scripts/prepare_dataset.py) 切分训练集
3. 按 [led_digits_rec_train.template.yml](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/configs/led_digits_rec_train.template.yml) 和 [water_meter_rec_train.template.yml](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/configs/water_meter_rec_train.template.yml) 开 PaddleOCR 训练
4. 再按 [paddleocr-android-integration-plan.md](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/docs/paddleocr-android-integration-plan.md) 把推理模型接进 `phiron-ocr`

## 当前最重要的事

不是继续调 UI 里的预处理开关，而是开始攒样本。

## 演示数据快速跑通

如果你只是想先验证训练流程能不能跑通，可以用：

[duplicate_demo_samples.sh](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/training/meter_ocr/scripts/duplicate_demo_samples.sh)

它会把当前两张样本各复制 100 份，并自动生成：

- `training/meter_ocr/annotations/labels.txt`

注意：

- 这只能验证训练命令和数据链路
- 不能训练出真正可用的模型
