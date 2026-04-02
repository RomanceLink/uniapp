# uniplugin_sunmiFace

Sunmi 人脸识别 uni-app 安卓原生插件。

## AAR 说明

`facelib-release.aar` 是正常的 Android SDK 交付格式，不需要手动改成 `.jar`。

- `.jar` 只能放 Java class
- `.aar` 还能包含 `AndroidManifest`、`res`、`jni/*.so`
- 人脸识别 SDK 这类带 native 库的包，通常都应该用 `.aar`

## 已封装接口

模块名：`sunmiFace`

- `initAuthorizeSDK({ debuggable }, callback)`
- `getAuthorizeSDKVersion()`
- `syncGetAuthorizeCode({ appId, forceRefresh }, callback)`
- `asyncGetAuthorizeToken({ appId, forceRefresh }, callback)`
- `clearLocalToken(callback)`
- `activateByAppId({ appId, forceRefresh }, callback)`
- `activateByLicensePath({ licensePath }, callback)`
- `checkPermissions(callback)`
- `createHandle(callback)`
- `init(options, callback)`
- `verifyLicense(options, callback)`
- `getVersion()`
- `getErrorString({ code })`
- `setConfig(options, callback)`
- `getConfig(callback)`
- `initDB(options, callback)`
- `getImageFeatures(options, callback)`
- `releaseImageFeatures({ token }, callback)`
- `addDBRecord(options, callback)`
- `searchDB(options, callback)`
- `compare1v1({ first, second }, callback)`
- `deleteDBRecord({ id }, callback)`
- `releaseHandle(callback)`

## Vue 页面调用示例

```js
const sunmiFace = uni.requireNativePlugin('sunmiFace')

sunmiFace.initAuthorizeSDK({ debuggable: true }, (res) => {
  console.log('initAuthorizeSDK', res)
})

sunmiFace.createHandle((res) => {
  console.log('createHandle', res)
})

sunmiFace.init({
  useAssetConfig: true
}, (res) => {
  console.log('init', res)
})
```

### 本地 license 文件激活

```js
sunmiFace.activateByLicensePath({
  licensePath: '/sdcard/SunmiRemoteFiles/license_face.txt'
}, (res) => {
  console.log('activateByLicensePath', res)
})
```

### App ID 拉 token 激活

```js
sunmiFace.activateByAppId({
  appId: '你的商米AppId',
  forceRefresh: false
}, (res) => {
  console.log('activateByAppId', res)
})
```

### 激活成功后再调用 facelib 接口

```js

sunmiFace.setConfig({
  threadNum: 2,
  distanceThreshold: 0.6,
  faceScoreThreshold: 0.8,
  minFaceSize: 80,
  boxSortMode: 0
}, (res) => {
  console.log('setConfig', res)
})

sunmiFace.getImageFeatures({
  imagePath: '/storage/emulated/0/Download/test.jpg',
  maxFaceCount: 1,
  predictMode: 3,
  livenessMode: 0,
  qualityMode: 0,
  keepAlive: true
}, (res) => {
  console.log('getImageFeatures', res)
})
```

## 关键参数

`getImageFeatures / addDBRecord / searchDB / compare1v1` 支持两种输入方式：

- 直接传 `feature: number[]`
- 传 `imagePath` 或 `base64`，插件内部自动提取特征

如果 `getImageFeatures` 传了 `keepAlive: true`，返回值里会带 `token`，后续可传给：

- `addDBRecord({ id, token })`
- `searchDB({ token })`
- `releaseImageFeatures({ token })`
