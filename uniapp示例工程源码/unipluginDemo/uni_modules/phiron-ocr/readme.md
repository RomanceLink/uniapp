# phiron-ocr

面向电子称图片数值识别的 UTS 插件，Android 端提供：

- OpenCV 图片预处理：灰度化、降噪、锐化、对比度增强、自适应二值化、透视矫正、旋转矫正
- OCR 识别：返回文字、启发式置信度、文本框坐标
- 电子称预设：自动偏向数字、支持数值候选提取，直接返回数值数组

## 调用示例

```ts
import * as PhironOcr from '@/uni_modules/phiron-ocr'

const result = PhironOcr.recognizeScaleValue({
  imagePath: '/storage/emulated/0/DCIM/scale.jpg',
  preprocess: {
    enableDeskew: true,
    enableAdaptiveThreshold: true
  }
})

console.log(result.data.primaryValue)
console.log(result.data.values)
```

## 返回结果重点字段

- `data.primaryValue`: 主识别值，优先取最可信的数值
- `data.values`: 当前图片中识别到的数值列表
- `data.recognizedValues`: 带 `value/confidence/box/sourceText` 的完整结构

## 当前说明

- 当前 OCR 底层使用 `ML Kit Text Recognition`
- `confidence` 为面向电子称数字场景的启发式分数，不是模型原生概率
- 后续如果切换 `PaddleOCR / Tesseract`，前端接口可以保持不变
