# phiron-sunmiFace

商米人脸识别 UTS 插件。

## 当前迁移状态

这个目录已经从 UTS 模板初始化为 Android 版 UTS 插件，当前已完成：

1. 插件基本清单已调整为 Android 可发布状态
2. 已补充 Android 依赖库
3. 已补充 Android 权限配置
4. 已基于商米底层 SDK 重写大部分 Android 接口

当前已放入的 Android 本地依赖：

- `facelib-release.aar`
- `opencv.aar`
- `SunmiAuthorize-SDK-1.0.1.aar`

路径位置：

- [libs](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/phiron-sunmiFace/utssdk/app-android/libs)

## 迁移说明

原有插件是旧版 Android 原生插件，核心入口是：

- [SunmiFaceModule.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceModule.java)

本次迁移不再依赖旧插件封装产物，而是直接基于底层商米 SDK 重新实现 UTS 接口。

官方文档说明：

1. UTS 插件可以直接封装现有 `aar/jar`
2. 前端应通过 `@/uni_modules/插件名` 的方式导入调用

参考文档：

- [UTS 插件介绍](https://uniapp.dcloud.net.cn/plugin/uts-plugin.html)
- [UTS for Android](https://uniapp.dcloud.net.cn/plugin/uts-for-android.html)

## 当前已实现接口

当前 Android 侧已实现：

1. 基础能力
   - `getVersion`
   - `getDeviceInfo`
   - `checkPermissions`
   - `createHandle`
   - `init`
   - `releaseHandle`

2. 授权能力
   - `initAuthorizeSDK`
   - `getAuthorizeSDKVersion`
   - `syncGetAuthorizeCode`
   - `asyncGetAuthorizeToken`
   - `activateByLicensePath`
   - `activateByAppId`
   - `verifyLicense`
   - `clearLocalToken`

3. 图片识别能力
   - `getImageFeatures`
   - `releaseImageFeatures`
   - `searchDB`
   - `addDBRecord`
   - `compare1v1`

4. 人脸库能力
   - `initDB`
   - `getAllDBRecords`
   - `deleteDBRecord`
   - `clearFaceDatabase`

5. 实时识别能力
   - `startFaceDetect`
   - `stopFaceDetect`
   - `openFaceDetect`
   - `startFaceRecognize`

## 当前说明

1. `startFaceDetect`
   - 用于前端页面内悬浮窗实时识别

2. `openFaceDetect`
   - 用于全屏原生实时识别

3. `startFaceRecognize`
   - 当前先作为全屏实时识别兼容别名实现
   - 便于旧前端调用方式平滑迁移

4. `checkPermissions`
   - 当前先返回权限申请建议
   - 建议在页面层自行申请 `CAMERA`、存储、设备信息相关权限

## 前端最终目标用法

迁移完成后，前端不再使用：

```js
const sunmiFace = uni.requireNativePlugin('sunmiFace')
```

而改成：

```js
import * as SunmiFace from '@/uni_modules/phiron-sunmiFace'
```

或：

```js
import {
  startFaceDetect,
  openFaceDetect,
  getImageFeatures,
  searchDB
} from '@/uni_modules/phiron-sunmiFace'
```

## 提醒

1. 当前代码已经完成 UTS 目录迁移和 Android 实现落地
2. 由于本地环境没有 HBuilderX 的 UTS 编译链，这一版还需要在 HBuilderX 中进行首次真机编译验证
3. 如果首次编译出现 UTS/Kotlin 适配报错，再按编译器提示继续收口即可
