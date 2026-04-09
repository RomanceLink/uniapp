# 商米人脸识别反复打开后闪退问题记录

## 问题现象

在商米已授权设备上，走这条链路时容易闪退：

1. 打开原生相机识别
2. 实时检测
3. 人脸识别
4. 搜库返回结果
5. 自动关闭
6. 再次打开或第二次识别成功后闪退

表现特征：

- 普通未授权手机通常不会闪退，只会返回 `License is not valid`
- 商米已授权设备更容易在“识别成功后”或“反复打开关闭几次后”闪退
- 问题更像 native 生命周期问题，不像单纯 Java 异常

## 最终确认有效的修复点

这次真正让问题明显缓解/消失的核心修改有 2 个。

### 1. 不再在实时识别每次关闭时立刻释放 SDK 句柄

文件：

- [SunmiFaceCameraView.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceCameraView.java)

关键点：

- 引入共享 SDK 状态：
  - `sharedSdkHandleReady`
  - `sharedSdkLicenseVerified`
  - `sharedAuthorizeSdkReady`
  - `sharedInitializedDbPath`
- `scheduleSdkRelease(...)` 改为空实现，不再在每次关闭实时相机时调用 `releaseHandle`
- 改成只在显式调用模块 `releaseHandle` 时，才真正执行 `SunmiFaceSDK.releaseHandle()`

原因：

- 商米设备在授权成功后，反复 `createHandle -> releaseHandle -> createHandle`，非常容易在第二次、第三次打开识别时触发 native 崩溃
- 实时识别线程和关闭流程可能有交叉，过早释放句柄风险很高

当前代码位置：

- [SunmiFaceCameraView.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceCameraView.java):880
- [SunmiFaceCameraView.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceCameraView.java):893

### 2. 去掉对 `SunmiFaceDBRecord` 的手动释放

文件：

- [SunmiFaceCameraView.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceCameraView.java)
- [SunmiFaceModule.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceModule.java)

关键点：

- 保留：
  - `feature.delete()`
  - `SunmiFaceSDK.releaseImageFeatures(...)`
- 去掉：
  - `record.delete()`

原因：

- 官方文档明确提到需要手动释放的是：
  - `SunmiFaceFeatureArrayGetItem(...)` 取出的对象
  - `SunmiFaceLmkArrayGetItem(...)` 取出的对象
  - `SunmiFaceImageFeatures`
  - `releaseHandle`
- 官方示例没有明确要求对 `faceFeature2FaceDBRecord(...)` 返回的 `SunmiFaceDBRecord` 手动 `delete()`
- 问题只在“识别成功 / 搜库成功后”更容易出现，也和这条释放链高度相关

当前代码位置：

- [SunmiFaceModule.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceModule.java):631
- [SunmiFaceModule.java](/Users/panlong/AndroidStudioProjects/Android-SDK/UniPlugin-Hello-AS/uniplugin_sunmiFace/src/main/java/com/example/uniplugin_sunmiface/SunmiFaceModule.java):649

## 当前推荐的释放策略

### 实时相机识别链

- 首次进入时创建共享 `handle`
- 识别过程中复用同一个 `handle`
- 每帧处理结束后：
  - `feature.delete()`
  - `SunmiFaceSDK.releaseImageFeatures(imageFeatures)`
- 关闭相机时：
  - 只停预览，不释放共享 `handle`
- 只有业务明确调用插件 `releaseHandle` 时：
  - 再统一 `SunmiFaceSDK.releaseHandle()`

### 手动图片识别链

- `getImageFeatures` 返回后，按 token 或 owned 生命周期管理
- `FeatureCarrier` 释放时：
  - 释放 `feature`
  - 释放 owned 的 `imageFeatures`
- 不手动释放 `record`

## 为什么未授权手机不闪退

因为未授权手机通常走不到真正的“成功识别 -> 搜库 -> 成功返回 -> 成功关闭”这条完整链路。

也就是说它大多停在：

- `verifyLicense` 失败
- 或 `getImageFeatures / searchDB` 直接报未授权

这类场景没有把成功路径上的高风险 native 生命周期完整走一遍，所以反而不容易触发崩溃。

## 后续排查建议

如果后面又出现类似闪退，优先检查这几件事：

1. 是否又把实时相机关闭时的 `releaseHandle` 加回来了
2. 是否又对 `SunmiFaceDBRecord` 做了手动 `delete()`
3. 是否在识别线程还未完全结束时就释放了 SDK 句柄
4. 是否在成功搜库后，又增加了新的 native 对象主动释放逻辑

## 结论

这次问题的本质更像：

- 商米人脸 SDK 在授权成功后的 native 生命周期比较敏感
- 尤其怕“成功路径上的额外释放”和“反复开关相机时频繁释放 handle”

本次稳定版本的关键不是调图片质量，而是：

- **共享复用 SDK handle，不在每次关相机时释放**
- **只释放官方明确要求释放的对象，不额外释放 `SunmiFaceDBRecord`**
