# Sunmi Face 插件使用说明

本文档基于当前项目中的插件实现与前端示例页整理：

## 1. 插件引用方式

在 `uni-app` 页面中引用插件：

```js
const pluginName = 'sunmiFace'
let sunmiFace = null

// #ifdef APP-PLUS
sunmiFace = uni.requireNativePlugin(pluginName)
// #endif
```

建议先做运行环境判断：

```js
function checkRuntime() {
  // #ifndef APP-PLUS
  uni.showToast({ title: '请在 App 端测试', icon: 'none' })
  return false
  // #endif
  if (!sunmiFace) {
    uni.showToast({ title: '插件加载失败', icon: 'none' })
    return false
  }
  return true
}
```

## 2. 推荐接入顺序

推荐按照下面顺序调用：

1. `checkPermissions`
2. `createHandle`
3. `init`
4. 激活授权
   - `activateByLicensePath`
   - 或 `activateByAppId`
5. 如果要做图片识别，先 `getImageFeatures`
6. 如果要做人脸库，先 `initDB`
7. 再调用：
   - `addDBRecord`
   - `searchDB`
   - `compare1v1`
   - `getAllDBRecords`
   - `deleteDBRecord`
   - `clearFaceDatabase`

如果要做实时相机识别：

1. `checkPermissions`
2. `createHandle`
3. `init`
4. 激活授权
5. 调 `startFaceDetect` 或 `openFaceDetect`

## 3. 授权方式

插件支持两种授权方式。

### 3.1 通过 license 文件激活

```js
sunmiFace.activateByLicensePath({
  licensePath: '/storage/emulated/0/SunmiRemoteFiles/license_face.txt'
}, (res) => {
  console.log('activateByLicensePath', res)
})
```

### 3.2 通过 AppId 激活

```js
sunmiFace.activateByAppId({
  appId: '你的商米AppId',
  forceRefresh: false
}, (res) => {
  console.log('activateByAppId', res)
})
```

### 3.3 授权相关辅助接口

#### 初始化授权 SDK

```js
sunmiFace.initAuthorizeSDK({
  debuggable: true
}, (res) => {
  console.log(res)
})
```

#### 获取授权 SDK 版本

```js
const res = sunmiFace.getAuthorizeSDKVersion()
console.log(res)
```

#### 拉取授权码

```js
sunmiFace.syncGetAuthorizeCode({
  appId: '你的商米AppId',
  forceRefresh: false
}, (res) => {
  console.log(res)
})
```

#### 清理本地授权

```js
sunmiFace.clearLocalToken((res) => {
  console.log(res)
})
```

## 4. 初始化与基础接口

### 4.1 获取设备信息

```js
const res = sunmiFace.getDeviceInfo()
console.log(res)
```

### 4.2 获取 SDK 版本

```js
const res = sunmiFace.getVersion()
console.log(res)
```

### 4.3 申请权限

```js
sunmiFace.checkPermissions((res) => {
  console.log(res)
})
```

### 4.4 创建句柄

```js
sunmiFace.createHandle((res) => {
  console.log(res)
})
```

### 4.5 初始化模型

```js
sunmiFace.init({
  useAssetConfig: true
}, (res) => {
  console.log(res)
})
```

### 4.6 释放句柄

```js
sunmiFace.releaseHandle((res) => {
  console.log(res)
})
```

## 5. 实时相机识别

当前支持两种模式：

1. `startFaceDetect`
   页面内悬浮识别窗
2. `openFaceDetect`
   全屏原生识别页

### 5.1 悬浮窗识别

```js
sunmiFace.startFaceDetect({
  appId: '',
  licensePath: '/storage/emulated/0/SunmiRemoteFiles/license_face.txt',
  forceRefresh: false,
  floatingWindowMode: true,
  containerBackgroundColor: 'transparent',
  windowWidthRatio: 0.58,
  windowHeightRatio: 0.42,
  windowOffsetXRatio: 0,
  windowOffsetYRatio: -0.06,
  cameraFacing: 'front',
  showCloseButton: true,
  showStartButton: false,
  showStatusText: true,
  autoStartAnalyze: true,
  autoStopOnRecognize: true,
  maxRecognizeFailures: 3,
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 0,
  maxFaceCount: 1,
  faceScoreThreshold: 0.7,
  threadNum: 2,
  analyzeIntervalMs: 1000,
  previewDecodeMaxSize: 640,
  displayOrientationDeg: 360,
  captureImageRotationDeg: 360,
  captureMirrorX: false,
  rectRotation: 0,
  rectMirrorX: false,
  showSquareGuide: true,
  showGuideMask: false,
  guideBoxWidthRatio: 0.62,
  guideBoxHeightRatio: 0.62,
  guideOffsetXRatio: 0,
  guideOffsetYRatio: 0,
  dbPath: ''
}, (res) => {
  console.log('startFaceDetect', res)
})
```

关闭悬浮窗：

```js
const res = sunmiFace.stopFaceDetect()
console.log(res)
```

### 5.2 全屏原生识别页

```js
sunmiFace.openFaceDetect({
  appId: '',
  licensePath: '/storage/emulated/0/SunmiRemoteFiles/license_face.txt',
  forceRefresh: false,
  cameraFacing: 'front',
  showCloseButton: false,
  showStartButton: false,
  showStatusText: false,
  autoStartAnalyze: true,
  autoStopOnRecognize: true,
  maxRecognizeFailures: 3,
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 0,
  maxFaceCount: 1
}, (res) => {
  console.log('openFaceDetect', res)
})
```

### 5.3 实时事件说明

实时识别过程中，会通过回调不断返回事件对象。常见 `eventType`：

1. `preview_ready`
2. `detecting`
3. `face_not_detected`
4. `face_detected`
5. `face_occluded`
6. `face_pose_invalid`
7. `face_too_dark`
8. `face_blurry`
9. `face_not_live`
10. `recognize_failed`
11. `recognize_success`
12. `recognize_max_failures`
13. `sdk_error`

## 6. 实时相机识别参数说明

### 6.1 相机与方向

- `cameraFacing`
  `front` 或 `back`
- `displayOrientationDeg`
  预览方向，可传 `0 / 90 / 180 / 270 / 360`
  `360` 表示自动
- `captureImageRotationDeg`
  抓拍图片方向，可传 `0 / 90 / 180 / 270 / 360`
  `360` 表示自动
- `captureMirrorX`
  抓拍图是否镜像
- `rectRotation`
  返回的人脸框坐标旋转
- `rectMirrorX`
  返回的人脸框坐标是否镜像

### 6.2 悬浮窗布局

仅 `startFaceDetect` 有意义：

- `floatingWindowMode`
  是否启用页面中间悬浮窗
- `containerBackgroundColor`
  容器背景色，如 `transparent`、`#22000000`
- `windowWidthRatio`
  悬浮窗宽度占屏幕宽度比例
- `windowHeightRatio`
  悬浮窗高度占屏幕高度比例
- `windowOffsetXRatio`
  悬浮窗横向偏移比例
- `windowOffsetYRatio`
  悬浮窗纵向偏移比例

### 6.3 UI 控制

- `showCloseButton`
- `showStartButton`
- `showStatusText`
- `autoStartAnalyze`
- `autoStopOnRecognize`
- `maxRecognizeFailures`

### 6.4 引导框与遮罩

- `showSquareGuide`
- `showGuideMask`
- `guideBoxWidthRatio`
- `guideBoxHeightRatio`
- `guideOffsetXRatio`
- `guideOffsetYRatio`

### 6.5 识别参数

- `predictMode`
  - `1` 仅检测
  - `3` 特征识别
  - `5` 属性预测
- `livenessMode`
  - `0` 关闭
  - `1` RGB 活体
- `qualityMode`
  - `0` 关闭
  - `1` 姿态
  - `2` 遮挡
  - `3` 姿态+遮挡
- `maxFaceCount`
- `minFaceSize`
- `distanceThreshold`
- `faceScoreThreshold`
- `threadNum`
- `analyzeIntervalMs`
- `previewDecodeMaxSize`
- `dbPath`

## 7. 图片特征提取

### 7.1 提取图片特征

```js
sunmiFace.getImageFeatures({
  imagePath: '/storage/emulated/0/face_a.jpg',
  keepAlive: true,
  maxFaceCount: 1,
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 0,
  decodeMaxSize: 1280,
  rectRotation: 0,
  rectMirrorX: false
}, (res) => {
  console.log(res)
})
```

返回中如果 `keepAlive=true`，通常会拿到 `token`，后续可继续复用。

### 7.2 释放图片特征

```js
sunmiFace.releaseImageFeatures({
  token: 'xxxx'
}, (res) => {
  console.log(res)
})
```

## 8. 人脸库操作

### 8.1 初始化人脸库

```js
sunmiFace.initDB({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db'
}, (res) => {
  console.log(res)
})
```

### 8.2 入库

支持三种输入方式：

1. `token`
2. `feature`
3. `imagePath`

示例：

```js
sunmiFace.addDBRecord({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db',
  id: 'user-001',
  name: '张三',
  phone: '13800000000',
  imgId: '',
  photoPath: '/storage/emulated/0/face_a.jpg',
  token: 'xxxx'
}, (res) => {
  console.log(res)
})
```

### 8.3 搜库

```js
sunmiFace.searchDB({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db',
  token: 'xxxx'
}, (res) => {
  console.log(res)
})
```

### 8.4 获取全部记录

```js
sunmiFace.getAllDBRecords({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db'
}, (res) => {
  console.log(res)
})
```

### 8.5 删除记录

```js
sunmiFace.deleteDBRecord({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db',
  id: 'user-001',
  imgId: ''
}, (res) => {
  console.log(res)
})
```

### 8.6 清空人脸库

```js
sunmiFace.clearFaceDatabase({
  dbPath: '/storage/emulated/0/face_db/sunmi_face.db'
}, (res) => {
  console.log(res)
})
```

## 9. 1:1 比对

```js
sunmiFace.compare1v1({
  first: { token: 'token-a' },
  second: { token: 'token-b' }
}, (res) => {
  console.log(res)
})
```

也可以传 `feature` 或 `imagePath`。

## 10. 常用测试能力

### 10.1 人脸检测

```js
sunmiFace.getImageFeatures({
  imagePath: '/storage/emulated/0/face_a.jpg',
  predictMode: 1,
  livenessMode: 0,
  qualityMode: 0
}, (res) => {
  console.log(res)
})
```

### 10.2 人脸识别

```js
sunmiFace.searchDB({
  token: 'xxxx'
}, (res) => {
  console.log(res)
})
```

### 10.3 RGB 活体

```js
sunmiFace.getImageFeatures({
  imagePath: '/storage/emulated/0/face_a.jpg',
  predictMode: 3,
  livenessMode: 1,
  qualityMode: 0
}, (res) => {
  console.log(res)
})
```

### 10.4 质量评估

```js
sunmiFace.getImageFeatures({
  imagePath: '/storage/emulated/0/face_a.jpg',
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 3
}, (res) => {
  console.log(res)
})
```

## 11. 错误处理

### 11.1 获取错误字符串

```js
const res = sunmiFace.getErrorString({ code: 9 })
console.log(res)
```

### 11.2 常见问题

1. 手机上一直返回未授权
- 需要先激活 `licensePath` 或 `appId`
- 未授权手机通常只能测试接口流程，不能做完整识别

2. 商米设备识别成功后闪退
- 参考：
  [sunmi-face-crash-fix-record.md](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/docs/sunmi-face-crash-fix-record.md)

3. 人脸方向不对
- 重点检查：
  - `displayOrientationDeg`
  - `captureImageRotationDeg`
  - `captureMirrorX`
  - `rectRotation`
  - `rectMirrorX`

4. 实时检测状态很多但前端没处理
- 建议统一根据 `eventType` 做提示文案

## 12. 前端示例建议

推荐在业务侧封装一个统一调用方法，例如：

```js
function invokeFace(methodName, payload = {}) {
  return new Promise((resolve, reject) => {
    const plugin = uni.requireNativePlugin('sunmiFace')
    if (!plugin || typeof plugin[methodName] !== 'function') {
      reject(new Error(`插件未暴露方法: ${methodName}`))
      return
    }
    plugin[methodName](payload, (res) => resolve(res))
  })
}
```

## 13. 接口清单

当前插件已暴露接口如下：

1. `getVersion`
2. `getDeviceInfo`
3. `initAuthorizeSDK`
4. `getAuthorizeSDKVersion`
5. `syncGetAuthorizeCode`
6. `asyncGetAuthorizeToken`
7. `clearLocalToken`
8. `startFaceRecognize`
9. `startFaceDetect`
10. `stopFaceDetect`
11. `openFaceDetect`
12. `activateByAppId`
13. `activateByLicensePath`
14. `checkPermissions`
15. `createHandle`
16. `init`
17. `verifyLicense`
18. `getErrorString`
19. `setConfig`
20. `getConfig`
21. `initDB`
22. `getImageFeatures`
23. `releaseImageFeatures`
24. `addDBRecord`
25. `searchDB`
26. `compare1v1`
27. `deleteDBRecord`
28. `getAllDBRecords`
29. `clearFaceDatabase`
30. `releaseHandle`

## 14. 建议的支付场景配置

如果你要做人脸支付识别，推荐从下面这组参数起步：

```js
{
  cameraFacing: 'front',
  showCloseButton: false,
  showStartButton: false,
  showStatusText: false,
  autoStartAnalyze: true,
  autoStopOnRecognize: true,
  maxRecognizeFailures: 3,
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 0,
  maxFaceCount: 1,
  distanceThreshold: 0.9,
  minFaceSize: 60,
  faceScoreThreshold: 0.7,
  threadNum: 2,
  floatingWindowMode: true,
  containerBackgroundColor: 'transparent',
  windowWidthRatio: 0.58,
  windowHeightRatio: 0.42,
  windowOffsetXRatio: 0,
  windowOffsetYRatio: -0.06,
  showSquareGuide: true,
  showGuideMask: false
}
```
