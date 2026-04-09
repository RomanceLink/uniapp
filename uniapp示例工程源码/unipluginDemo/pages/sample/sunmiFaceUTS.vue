<template>
	<view class="page">
		<view class="scroll">
			<view class="card hero">
				<view class="title">Sunmi Face UTS 示例页</view>
				<view class="desc">这个页面演示如何通过 UTS 插件 `phiron-sunmiFace` 调用商米人脸 SDK，包含授权、初始化、图片识别、人脸库和实时识别。</view>
				<view class="guide">
					推荐顺序：1. 安装 UTS 插件 2. 申请权限 3. 创建句柄 4. 初始化模型 5. 通过文件激活/通过 AppId 激活 6. 提取图片特征 7. 初始化人脸库 8. 入库/搜索/删除
				</view>
			</view>

			<view class="card">
				<view class="section-title">第一步：初始化</view>
				<view class="section-tip">先完成设备检查、权限申请、句柄创建和模型初始化。</view>
				<view class="button-row">
					<button class="btn primary" size="mini" @click="runGetDeviceInfo">获取设备信息</button>
					<button class="btn" size="mini" @click="runGetVersion">获取 SDK 版本</button>
					<button class="btn" size="mini" @click="runCheckPermissions">申请权限</button>
					<button class="btn" size="mini" @click="runCreateHandle">创建句柄</button>
					<button class="btn" size="mini" @click="runInit">初始化模型</button>
					<button class="btn warn" size="mini" @click="runReleaseHandle">释放句柄</button>
				</view>
			</view>

			<view class="card">
				<view class="section-title">第二步：激活授权</view>
				<view class="section-tip">UTS 版支持同步授权码、异步授权码兼容接口、文件激活和 AppId 激活。初始化模型成功后再做授权。</view>
				<view class="field">
					<view class="label">AppId</view>
					<uni-easyinput v-model="form.appId" class="easy-input" placeholder="商米平台申请的 AppId" />
				</view>
				<view class="field">
					<view class="label">licensePath</view>
					<uni-easyinput v-model="form.licensePath" class="easy-input" placeholder="/storage/emulated/0/SunmiRemoteFiles/license_face.txt" />
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">useAssetConfig</view>
						<switch :checked="form.useAssetConfig" color="#2563eb" @change="onSwitch('useAssetConfig', $event)" />
					</view>
					<view class="col">
						<view class="label">forceRefresh</view>
						<switch :checked="form.forceRefresh" color="#2563eb" @change="onSwitch('forceRefresh', $event)" />
					</view>
				</view>
				<view class="button-row">
					<button class="btn" size="mini" @click="runInitAuthorizeSDK">初始化授权 SDK</button>
					<button class="btn" size="mini" @click="runGetAuthorizeSDKVersion">获取授权 SDK 版本</button>
					<button class="btn" size="mini" @click="runSyncGetAuthorizeCode">拉取授权码</button>
					<button class="btn" size="mini" @click="runAsyncGetAuthorizeToken">异步授权码</button>
					<button class="btn primary" size="mini" @click="runActivateByLicensePath">通过文件激活</button>
					<button class="btn primary" size="mini" @click="runActivateByAppId">通过 AppId 激活</button>
					<button class="btn warn" size="mini" @click="runClearLocalToken">清除本地授权</button>
				</view>
			</view>

			<view class="card">
				<view class="section-title">第三步：照片处理</view>
				<view class="section-tip">授权成功后，再选择图片。图片 A 用于检测/识别，图片 B 可用于人证比对。</view>
				<view class="field">
					<view class="label">UTS 实时人脸识别</view>
					<view class="section-tip">通过 `@/uni_modules/phiron-sunmiFace` 直接调用 `startFaceDetect / openFaceDetect / startFaceRecognize`。</view>
					<view class="button-row">
						<button class="btn primary" size="mini" @click="runStartFaceDetect">开始人脸识别（悬浮框）</button>
						<button class="btn" size="mini" @click="runStopFaceDetect">关闭人脸识别</button>
						<button class="btn" size="mini" @click="runOpenFaceDetect">人脸识别（原生界面）</button>
						<button class="btn" size="mini" @click="runStartFaceRecognize">兼容入口 startFaceRecognize</button>
					</view>
				</view>
				<view class="field">
					<view class="label">实时检测状态 faceEventMessage（直接来自 UTS 回调）</view>
					<view class="result-box">{{ faceEventMessage }}</view>
				</view>
				<view class="field">
					<view class="label">悬浮窗布局</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">启用悬浮窗模式</view>
							<switch :checked="form.floatingWindowMode" color="#2563eb" @change="onSwitch('floatingWindowMode', $event)" />
						</view>
						<view class="col">
							<view class="label">容器背景色</view>
							<uni-easyinput v-model="form.containerBackgroundColor" class="easy-input" placeholder="transparent / #22000000" />
						</view>
					</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">窗口宽度比例</view>
							<uni-easyinput v-model="form.windowWidthRatio" class="easy-input" placeholder="0.58" />
						</view>
						<view class="col">
							<view class="label">窗口高度比例</view>
							<uni-easyinput v-model="form.windowHeightRatio" class="easy-input" placeholder="0.42" />
						</view>
					</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">横向偏移比例</view>
							<uni-easyinput v-model="form.windowOffsetXRatio" class="easy-input" placeholder="0" />
						</view>
						<view class="col">
							<view class="label">纵向偏移比例</view>
							<uni-easyinput v-model="form.windowOffsetYRatio" class="easy-input" placeholder="-0.06" />
						</view>
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">UTS 相机方向</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: form.cameraFacing === 'front' }" size="mini" @click="setFormValue('cameraFacing', 'front')">前置</button>
							<button class="btn option" :class="{ active: form.cameraFacing === 'back' }" size="mini" @click="setFormValue('cameraFacing', 'back')">后置</button>
						</view>
					</view>
					<view class="col">
						<view class="label">显示取消按钮</view>
						<switch :checked="form.showCancelButton" color="#2563eb" @change="onSwitch('showCancelButton', $event)" />
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">显示切换摄像头按钮（兼容保留）</view>
						<switch :checked="form.showSwitchCameraButton" color="#2563eb" @change="onSwitch('showSwitchCameraButton', $event)" />
					</view>
					<view class="col">
						<view class="label">UTS 是否显示状态</view>
						<switch :checked="form.showStatusText" color="#2563eb" @change="onSwitch('showStatusText', $event)" />
					</view>
				</view>
				<view class="field">
					<view class="label">启用系统人脸检测（兼容保留，UTS 首版暂未接入）</view>
					<switch :checked="form.enableSystemFaceDetection" color="#2563eb" @change="onSwitch('enableSystemFaceDetection', $event)" />
				</view>
				<view class="field">
					<view class="label">检测触发方式</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: !!form.autoStartAnalyze }" size="mini" @click="setFormValue('autoStartAnalyze', true)">自动检测</button>
						<button class="btn option" :class="{ active: !form.autoStartAnalyze }" size="mini" @click="setFormValue('autoStartAnalyze', false)">手动开始检测</button>
					</view>
				</view>
				<view class="field">
					<view class="label">支付模式预设</view>
					<view class="button-row">
						<button class="btn primary" size="mini" @click="applyCashierPreset">一键切到收银支付模式</button>
						<button class="btn" size="mini" @click="applyDebugPreset">恢复调试模式</button>
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">显示开始按钮</view>
						<switch :checked="form.showStartButton" color="#2563eb" @change="onSwitch('showStartButton', $event)" />
					</view>
					<view class="col">
						<view class="label">识别成功自动关闭</view>
						<switch :checked="form.autoStopOnRecognize" color="#2563eb" @change="onSwitch('autoStopOnRecognize', $event)" />
					</view>
				</view>
				<view class="field">
					<view class="label">连续失败自动关闭</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.maxRecognizeFailures) === 0 }" size="mini" @click="setFormValue('maxRecognizeFailures', 0)">关闭</button>
						<button class="btn option" :class="{ active: Number(form.maxRecognizeFailures) === 3 }" size="mini" @click="setFormValue('maxRecognizeFailures', 3)">3次</button>
						<button class="btn option" :class="{ active: Number(form.maxRecognizeFailures) === 5 }" size="mini" @click="setFormValue('maxRecognizeFailures', 5)">5次</button>
					</view>
				</view>
				<view class="field">
					<view class="label">方形引导框</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">显示方框</view>
							<switch :checked="form.showSquareGuide" color="#2563eb" @change="onSwitch('showSquareGuide', $event)" />
						</view>
						<view class="col">
							<view class="label">显示状态文字</view>
							<switch :checked="form.showStatusText" color="#2563eb" @change="onSwitch('showStatusText', $event)" />
						</view>
					</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">原生遮罩</view>
							<switch :checked="form.showGuideMask" color="#2563eb" @change="onSwitch('showGuideMask', $event)" />
						</view>
						<view class="col">
							<view class="label">宽度比例</view>
							<uni-easyinput v-model="form.guideBoxWidthRatio" class="easy-input" placeholder="0.62" />
						</view>
					</view>
					<view class="field two-col">
						<view class="col">
							<view class="label">高度比例</view>
							<uni-easyinput v-model="form.guideBoxHeightRatio" class="easy-input" placeholder="0.62" />
						</view>
						<view class="col">
							<view class="label">横向偏移比例</view>
							<uni-easyinput v-model="form.guideOffsetXRatio" class="easy-input" placeholder="0" />
						</view>
					</view>
					<view class="field">
						<view class="label">纵向偏移比例</view>
						<uni-easyinput v-model="form.guideOffsetYRatio" class="easy-input" placeholder="0" />
					</view>
				</view>
				<view class="field">
					<view class="label">UTS 识别模式</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.predictMode) === 1 }" size="mini" @click="setFormValue('predictMode', 1)">仅检测</button>
						<button class="btn option" :class="{ active: Number(form.predictMode) === 3 }" size="mini" @click="setFormValue('predictMode', 3)">特征识别</button>
						<button class="btn option" :class="{ active: Number(form.predictMode) === 5 }" size="mini" @click="setFormValue('predictMode', 5)">属性预测</button>
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">UTS 活体模式</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.livenessMode) === 0 }" size="mini" @click="setFormValue('livenessMode', 0)">关闭</button>
							<button class="btn option" :class="{ active: Number(form.livenessMode) === 1 }" size="mini" @click="setFormValue('livenessMode', 1)">RGB</button>
						</view>
					</view>
					<view class="col">
						<view class="label">UTS 质量模式</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.qualityMode) === 0 }" size="mini" @click="setFormValue('qualityMode', 0)">关闭</button>
							<button class="btn option" :class="{ active: Number(form.qualityMode) === 1 }" size="mini" @click="setFormValue('qualityMode', 1)">姿态</button>
							<button class="btn option" :class="{ active: Number(form.qualityMode) === 2 }" size="mini" @click="setFormValue('qualityMode', 2)">遮挡</button>
							<button class="btn option" :class="{ active: Number(form.qualityMode) === 3 }" size="mini" @click="setFormValue('qualityMode', 3)">姿态+遮挡</button>
						</view>
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">最大人脸数</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.maxFaceCount) === 1 }" size="mini" @click="setFormValue('maxFaceCount', 1)">1张</button>
							<button class="btn option" :class="{ active: Number(form.maxFaceCount) === 2 }" size="mini" @click="setFormValue('maxFaceCount', 2)">2张</button>
							<button class="btn option" :class="{ active: Number(form.maxFaceCount) === 3 }" size="mini" @click="setFormValue('maxFaceCount', 3)">3张</button>
						</view>
					</view>
					<view class="col">
						<view class="label">识别检测间隔</view>
						<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.analyzeIntervalMs) === 300 }" size="mini" @click="setFormValue('analyzeIntervalMs', 300)">300ms</button>
						<button class="btn option" :class="{ active: Number(form.analyzeIntervalMs) === 500 }" size="mini" @click="setFormValue('analyzeIntervalMs', 500)">500ms</button>
						<button class="btn option" :class="{ active: Number(form.analyzeIntervalMs) === 1000 }" size="mini" @click="setFormValue('analyzeIntervalMs', 1000)">1000ms</button>
						<button class="btn option" :class="{ active: Number(form.analyzeIntervalMs) === 1500 }" size="mini" @click="setFormValue('analyzeIntervalMs', 1500)">1500ms</button>
						</view>
					</view>
				</view>
				<view class="field">
					<view class="label">UTS 预览解码尺寸</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.previewDecodeMaxSize) === 480 }" size="mini" @click="setFormValue('previewDecodeMaxSize', 480)">480</button>
						<button class="btn option" :class="{ active: Number(form.previewDecodeMaxSize) === 640 }" size="mini" @click="setFormValue('previewDecodeMaxSize', 640)">640</button>
						<button class="btn option" :class="{ active: Number(form.previewDecodeMaxSize) === 720 }" size="mini" @click="setFormValue('previewDecodeMaxSize', 720)">720</button>
						<button class="btn option" :class="{ active: Number(form.previewDecodeMaxSize) === 960 }" size="mini" @click="setFormValue('previewDecodeMaxSize', 960)">960</button>
					</view>
				</view>
				<view class="field">
					<view class="label">UTS 预览方向</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.displayOrientationDeg) === 0 }" size="mini" @click="setFormValue('displayOrientationDeg', 0)">0°</button>
						<button class="btn option" :class="{ active: Number(form.displayOrientationDeg) === 90 }" size="mini" @click="setFormValue('displayOrientationDeg', 90)">90°</button>
						<button class="btn option" :class="{ active: Number(form.displayOrientationDeg) === 180 }" size="mini" @click="setFormValue('displayOrientationDeg', 180)">180°</button>
						<button class="btn option" :class="{ active: Number(form.displayOrientationDeg) === 270 }" size="mini" @click="setFormValue('displayOrientationDeg', 270)">270°</button>
						<button class="btn option" :class="{ active: Number(form.displayOrientationDeg) === 360 }" size="mini" @click="setFormValue('displayOrientationDeg', 360)">360°</button>
					</view>
				</view>
				<view class="field">
					<view class="label">抓拍图片旋转</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.captureImageRotationDeg) === 0 }" size="mini" @click="setFormValue('captureImageRotationDeg', 0)">0°</button>
						<button class="btn option" :class="{ active: Number(form.captureImageRotationDeg) === 90 }" size="mini" @click="setFormValue('captureImageRotationDeg', 90)">90°</button>
						<button class="btn option" :class="{ active: Number(form.captureImageRotationDeg) === 180 }" size="mini" @click="setFormValue('captureImageRotationDeg', 180)">180°</button>
						<button class="btn option" :class="{ active: Number(form.captureImageRotationDeg) === 270 }" size="mini" @click="setFormValue('captureImageRotationDeg', 270)">270°</button>
						<button class="btn option" :class="{ active: Number(form.captureImageRotationDeg) === 360 }" size="mini" @click="setFormValue('captureImageRotationDeg', 360)">360°</button>
					</view>
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">抓拍是否水平镜像</view>
						<switch :checked="form.captureMirrorX" color="#2563eb" @change="onSwitch('captureMirrorX', $event)" />
					</view>
					<view class="col">
						<view class="label">人脸框旋转</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.rectRotation) === 0 }" size="mini" @click="setFormValue('rectRotation', 0)">0°</button>
							<button class="btn option" :class="{ active: Number(form.rectRotation) === 90 }" size="mini" @click="setFormValue('rectRotation', 90)">90°</button>
							<button class="btn option" :class="{ active: Number(form.rectRotation) === 180 }" size="mini" @click="setFormValue('rectRotation', 180)">180°</button>
							<button class="btn option" :class="{ active: Number(form.rectRotation) === 270 }" size="mini" @click="setFormValue('rectRotation', 270)">270°</button>
							<button class="btn option" :class="{ active: Number(form.rectRotation) === 360 }" size="mini" @click="setFormValue('rectRotation', 360)">360°</button>
						</view>
					</view>
				</view>
				<view class="field">
					<view class="label">人脸框是否水平镜像</view>
					<switch :checked="form.rectMirrorX" color="#2563eb" @change="onSwitch('rectMirrorX', $event)" />
				</view>
				<view class="field two-col">
					<view class="col">
						<view class="label">距离阈值</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.distanceThreshold) === 0.6 }" size="mini" @click="setFormValue('distanceThreshold', 0.6)">0.6</button>
							<button class="btn option" :class="{ active: Number(form.distanceThreshold) === 0.8 }" size="mini" @click="setFormValue('distanceThreshold', 0.8)">0.8</button>
							<button class="btn option" :class="{ active: Number(form.distanceThreshold) === 0.9 }" size="mini" @click="setFormValue('distanceThreshold', 0.9)">0.9</button>
							<button class="btn option" :class="{ active: Number(form.distanceThreshold) === 1.1 }" size="mini" @click="setFormValue('distanceThreshold', 1.1)">1.1</button>
						</view>
					</view>
					<view class="col">
						<view class="label">最小人脸尺寸</view>
						<view class="button-row">
							<button class="btn option" :class="{ active: Number(form.minFaceSize) === 40 }" size="mini" @click="setFormValue('minFaceSize', 40)">40</button>
							<button class="btn option" :class="{ active: Number(form.minFaceSize) === 60 }" size="mini" @click="setFormValue('minFaceSize', 60)">60</button>
							<button class="btn option" :class="{ active: Number(form.minFaceSize) === 80 }" size="mini" @click="setFormValue('minFaceSize', 80)">80</button>
							<button class="btn option" :class="{ active: Number(form.minFaceSize) === 120 }" size="mini" @click="setFormValue('minFaceSize', 120)">120</button>
						</view>
					</view>
				</view>
				<view class="field">
					<view class="label">最大解码尺寸</view>
					<view class="button-row">
						<button class="btn option" :class="{ active: Number(form.decodeMaxSize) === 640 }" size="mini" @click="setFormValue('decodeMaxSize', 640)">640</button>
						<button class="btn option" :class="{ active: Number(form.decodeMaxSize) === 720 }" size="mini" @click="setFormValue('decodeMaxSize', 720)">720</button>
						<button class="btn option" :class="{ active: Number(form.decodeMaxSize) === 960 }" size="mini" @click="setFormValue('decodeMaxSize', 960)">960</button>
						<button class="btn option" :class="{ active: Number(form.decodeMaxSize) === 1280 }" size="mini" @click="setFormValue('decodeMaxSize', 1280)">1280</button>
					</view>
				</view>
				<view class="button-row">
					<button class="btn primary" size="mini" @click="runStartFaceDetect">开始相机识别</button>
					<button class="btn primary" size="mini" @click="chooseImage('a')">选择照片</button>
				</view>
				<view class="field">
					<view class="label">图片路径</view>
					<uni-easyinput v-model="imageA.imagePath" class="easy-input" placeholder="图片路径" />
				</view>
				<view class="field">
					<view class="label">特征令牌</view>
					<uni-easyinput :value="imageA.token" class="easy-input" disabled />
				</view>
				<view v-if="imageA.preview" class="preview-wrap">
					<image :src="imageA.preview" mode="aspectFit" class="preview"></image>
				</view>
				<view class="button-row">
					<button class="btn" size="mini" @click="runGetImageFeatures('a')">提取照片特征</button>
					<button class="btn warn" size="mini" @click="runReleaseImageFeatures('a')">释放照片特征</button>
				</view>
				<view class="field">
					<view class="label">身份证/对比照片 B</view>
					<uni-easyinput v-model="imageB.imagePath" class="easy-input" placeholder="用于人证比对的第二张照片路径" />
				</view>
				<view class="button-row">
					<button class="btn" size="mini" @click="chooseImage('b')">选择照片 B</button>
				</view>
			</view>

			<view class="card">
				<view class="section-title">第五步：能力测试</view>
				<view class="section-tip">下面这些按钮会按不同模式调用同一个底层 SDK，方便逐项验证功能。</view>
				<view class="button-row">
					<button class="btn primary" size="mini" @click="runFaceDetect">人脸检测</button>
					<button class="btn" size="mini" @click="runFaceRecognition">人脸识别</button>
					<button class="btn" size="mini" @click="runIdCompare">人证比对</button>
					<button class="btn" size="mini" @click="runFaceAttribute">人脸属性预测</button>
					<button class="btn" size="mini" @click="runRgbLiveness">RGB活体检测</button>
					<button class="btn" size="mini" @click="runQualityCheck">人脸质量评估</button>
				</view>
			</view>

			<view class="card">
				<view class="section-title">第四步：人脸库操作</view>
				<view class="section-tip">先初始化人脸库，再把图片 A 入库，然后可搜索和删除。</view>
				<view class="field">
					<view class="label">dbPath</view>
					<uni-easyinput v-model="form.dbPath" class="easy-input" placeholder="留空使用插件默认路径" />
				</view>
				<view class="field">
					<view class="label">id</view>
					<uni-easyinput v-model="form.dbRecord.id" class="easy-input" placeholder="数据库记录 id" />
				</view>
				<view class="field">
					<view class="label">name</view>
					<uni-easyinput v-model="form.dbRecord.name" class="easy-input" placeholder="名称" />
				</view>
				<view class="field">
					<view class="label">phone</view>
					<uni-easyinput v-model="form.dbRecord.phone" class="easy-input" placeholder="手机号" />
				</view>
				<view class="field">
					<view class="label">imgId</view>
					<uni-easyinput v-model="form.dbRecord.imgId" class="easy-input" placeholder="图片标识（可留空，成功后会自动生成）" />
				</view>
				<view class="field">
					<view class="label">photoPath</view>
					<uni-easyinput v-model="form.dbRecord.photoPath" class="easy-input" placeholder="留空则默认使用图片 A 路径" />
				</view>
				<view class="button-row">
					<button class="btn" size="mini" @click="runInitDB">初始化人脸库</button>
					<button class="btn" size="mini" @click="runAddDBRecord">图片 A 入库</button>
					<button class="btn" size="mini" @click="runSearchDB">图片 A 搜索</button>
					<button class="btn" size="mini" @click="runGetAllDBRecords">获取全部人脸</button>
					<button class="btn warn" size="mini" @click="runDeleteDBRecord">删除库记录</button>
					<button class="btn warn" size="mini" @click="runClearFaceDatabase">清空人脸库</button>
				</view>
			</view>

			<view class="card">
				<view class="section-title">最近结果</view>
				<view class="result-box">{{ prettyResult }}</view>
			</view>
		</view>
	</view>
</template>

<script>
let sunmiFace = null

const AUTO_ROTATION = 360

// #ifdef APP-PLUS
import * as SunmiFaceUTS from '@/uni_modules/phiron-sunmiFace'
sunmiFace = SunmiFaceUTS
// #endif

const createImageState = () => ({
	imagePath: '',
	base64: '',
	maxFaceCount: 1,
	predictMode: 3,
	livenessMode: 0,
	qualityMode: 0,
	keepAlive: true,
	token: '',
	feature: null,
	preview: ''
})

export default {
	data() {
		return {
			lastResult: null,
			faceEventMessage: '未开始识别',
			form: {
				appId: '',
				licensePath: '/storage/emulated/0/SunmiRemoteFiles/license_face.txt',
				useAssetConfig: true,
				forceRefresh: false,
				floatingWindowMode: true,
				containerBackgroundColor: 'transparent',
				windowWidthRatio: '0.58',
				windowHeightRatio: '0.42',
				windowOffsetXRatio: '0',
				windowOffsetYRatio: '-0.06',
				cameraFacing: 'front',
				showCancelButton: false,
				showSwitchCameraButton: false,
				showStatusText: true,
				showStartButton: false,
				enableSystemFaceDetection: false,
				autoStartAnalyze: true,
				autoStopOnRecognize: true,
				maxRecognizeFailures: 3,
				predictMode: 3,
				livenessMode: 0,
				qualityMode: 0,
				maxFaceCount: 1,
				analyzeIntervalMs: 1000,
				previewDecodeMaxSize: 640,
				displayOrientationDeg: AUTO_ROTATION,
				captureImageRotationDeg: AUTO_ROTATION,
				captureMirrorX: false,
				rectRotation: 0,
				rectMirrorX: false,
				showCircleGuide: false,
				showSquareGuide: true,
				showRedLineGuide: false,
				showGuideMask: false,
				guideBoxWidthRatio: '0.62',
				guideBoxHeightRatio: '0.62',
				guideOffsetXRatio: '0',
				guideOffsetYRatio: '0',
				distanceThreshold: '',
				minFaceSize: '',
				decodeMaxSize: 640,
				dbPath: '',
				dbRecord: {
					id: 'test-user-001',
					name: '测试用户',
					phone: '',
					imgId: '',
					photoPath: ''
				}
			},
			imageA: createImageState(),
			imageB: createImageState()
		}
	},
	onLoad() {},
	onUnload() {
	},
	computed: {
		prettyResult() {
			if (!this.lastResult) {
				return '暂无结果'
			}
			try {
				return JSON.stringify(this.lastResult, null, 2)
			} catch (e) {
				return String(this.lastResult)
			}
		},
	},
	methods: {
		checkRuntime() {
			// #ifndef APP-PLUS
			uni.showToast({ title: '请在 App 端测试', icon: 'none' })
			return false
			// #endif
			if (!sunmiFace) {
				uni.showToast({ title: 'UTS 插件加载失败', icon: 'none' })
				return false
			}
			return true
		},
		onSwitch(key, event) {
			this.form[key] = !!event.detail.value
		},
		setFormValue(key, value) {
			this.form[key] = value
		},
		setResult(title, payload) {
			this.lastResult = { title, payload }
		},
		toNumber(value, fallback = 0) {
			const num = Number(value)
			return Number.isFinite(num) ? num : fallback
		},
		clearImageState(type = 'a', options = {}) {
			const target = type === 'b' ? this.imageB : this.imageA
			const keepPreview = !!options.keepPreview
			if (!keepPreview) {
				target.preview = ''
				target.imagePath = ''
			}
			target.base64 = ''
			target.token = ''
			target.feature = null
		},
		clearImageToken(type = 'a') {
			const target = type === 'b' ? this.imageB : this.imageA
			target.token = ''
		},
		syncFeatureState(type, res) {
			const target = type === 'b' ? this.imageB : this.imageA
			const data = res && res.data ? res.data : null
			target.token = data && data.token ? data.token : ''
			target.feature = data && data.feature && data.feature.feature ? data.feature.feature : null
			return data
		},
		buildCommonFaceOptions() {
			const options = {
				rectRotation: this.toNumber(this.form.rectRotation, 0),
				rectMirrorX: !!this.form.rectMirrorX,
				decodeMaxSize: this.toNumber(this.form.decodeMaxSize, 1280)
			}
			const distanceThreshold = this.toNumber(this.form.distanceThreshold, 0)
			const minFaceSize = this.toNumber(this.form.minFaceSize, 0)
			if (distanceThreshold > 0) {
				options.distanceThreshold = distanceThreshold
			}
			if (minFaceSize > 0) {
				options.minFaceSize = minFaceSize
			}
			return options
		},
		buildNativeDetectPayload() {
			return {
				appId: this.form.appId || '',
				licensePath: this.form.licensePath || '',
				forceRefresh: !!this.form.forceRefresh,
				floatingWindowMode: !!this.form.floatingWindowMode,
				containerBackgroundColor: this.form.containerBackgroundColor || 'transparent',
				windowWidthRatio: this.toNumber(this.form.windowWidthRatio, 0.58),
				windowHeightRatio: this.toNumber(this.form.windowHeightRatio, 0.42),
				windowOffsetXRatio: this.toNumber(this.form.windowOffsetXRatio, 0),
				windowOffsetYRatio: this.toNumber(this.form.windowOffsetYRatio, -0.06),
				cameraFacing: this.form.cameraFacing || 'front',
				showCloseButton: !!this.form.showCancelButton,
				showCancelButton: !!this.form.showCancelButton,
				showStartButton: !!this.form.showStartButton,
				showStatusText: !!this.form.showStatusText,
				displayOrientationDeg: this.toNumber(this.form.displayOrientationDeg, AUTO_ROTATION),
				captureImageRotationDeg: this.toNumber(this.form.captureImageRotationDeg, AUTO_ROTATION),
				captureMirrorX: !!this.form.captureMirrorX,
				analyzeIntervalMs: this.toNumber(this.form.analyzeIntervalMs, 1000),
				previewDecodeMaxSize: this.toNumber(this.form.previewDecodeMaxSize, 640),
				predictMode: this.toNumber(this.form.predictMode, 3),
				livenessMode: this.toNumber(this.form.livenessMode, 0),
				qualityMode: this.toNumber(this.form.qualityMode, 0),
				maxFaceCount: this.toNumber(this.form.maxFaceCount, 1),
				faceScoreThreshold: 0.7,
				threadNum: 2,
				autoStopOnRecognize: !!this.form.autoStopOnRecognize,
				maxRecognizeFailures: this.toNumber(this.form.maxRecognizeFailures, 0),
				showCircleGuide: !!this.form.showCircleGuide,
				showSquareGuide: !!this.form.showSquareGuide,
				showRedLineGuide: !!this.form.showRedLineGuide,
				showGuideMask: !!this.form.showGuideMask,
				guideBoxWidthRatio: this.toNumber(this.form.guideBoxWidthRatio, 0.62),
				guideBoxHeightRatio: this.toNumber(this.form.guideBoxHeightRatio, 0.62),
				guideOffsetXRatio: this.toNumber(this.form.guideOffsetXRatio, 0),
				guideOffsetYRatio: this.toNumber(this.form.guideOffsetYRatio, 0),
				rectRotation: this.toNumber(this.form.rectRotation, 0),
				rectMirrorX: !!this.form.rectMirrorX,
				dbPath: this.form.dbPath || '',
				...this.buildCommonFaceOptions(),
			}
		},
		invoke(methodName, payload) {
			return new Promise((resolve, reject) => {
				if (!this.checkRuntime()) {
					reject(new Error('runtime not supported'))
					return
				}
				try {
					const fn = sunmiFace[methodName]
					if (typeof fn !== 'function') {
						reject(new Error(`插件未暴露方法: ${methodName}`))
						return
					}
					const res = typeof payload === 'undefined' ? fn() : fn(payload)
					resolve(res)
				} catch (e) {
					reject(e)
				}
			})
		},
		async invokeAndStore(title, methodName, payload) {
			try {
				const res = await this.invoke(methodName, payload)
				this.setResult(title, { request: payload, response: res })
				return res
			} catch (e) {
				this.setResult(`${title} 失败`, { request: payload, error: e.message || String(e) })
				throw e
			}
		},
		invokeAndStream(title, methodName, payload) {
			return new Promise((resolve, reject) => {
				if (!this.checkRuntime()) {
					reject(new Error('runtime not supported'))
					return
				}
				try {
					const fn = sunmiFace[methodName]
					if (typeof fn !== 'function') {
						reject(new Error(`插件未暴露方法: ${methodName}`))
						return
					}
					const openResult = fn(payload, (res) => {
						if (res && res.message) this.faceEventMessage = res.message
						this.setResult(title, { request: payload, response: res })
						if (res && res.terminal) {
							resolve(res)
						}
					})
					this.setResult(`${title} 已启动`, { request: payload, response: openResult })
				} catch (e) {
					reject(e)
				}
			})
		},
		buildInitPayload() {
			return {
				useAssetConfig: !!this.form.useAssetConfig
			}
		},
		buildAuthorizePayload() {
			return {
				appId: this.form.appId,
				forceRefresh: !!this.form.forceRefresh
			}
		},
		buildImagePayload(type = 'a', override = {}) {
			const image = type === 'b' ? this.imageB : this.imageA
			const payload = {
				keepAlive: !!image.keepAlive,
				maxFaceCount: this.toNumber(image.maxFaceCount, 1),
				predictMode: this.toNumber(image.predictMode, 3),
				livenessMode: this.toNumber(image.livenessMode, 0),
				qualityMode: this.toNumber(image.qualityMode, 0),
				...this.buildCommonFaceOptions()
			}
			if (image.imagePath) payload.imagePath = image.imagePath
			if (image.base64) payload.base64 = image.base64
			return Object.assign(payload, override)
		},
		buildFeaturePayload(type = 'a') {
			const image = type === 'b' ? this.imageB : this.imageA
			if (image.token) {
				return { token: image.token }
			}
			if (image.feature && image.feature.length) {
				return { feature: image.feature }
			}
			return this.buildImagePayload(type)
		},
		runGetDeviceInfo() {
			if (!this.checkRuntime()) return
			try {
				this.setResult('getDeviceInfo', sunmiFace.getDeviceInfo())
			} catch (e) {
				this.setResult('getDeviceInfo 失败', { error: e.message || String(e) })
			}
		},
		runGetVersion() {
			if (!this.checkRuntime()) return
			try {
				this.setResult('getVersion', sunmiFace.getVersion())
			} catch (e) {
				this.setResult('getVersion 失败', { error: e.message || String(e) })
			}
		},
		runCheckPermissions() {
			this.invokeAndStore('checkPermissions', 'checkPermissions')
		},
		runCreateHandle() {
			this.invokeAndStore('createHandle', 'createHandle')
		},
		runInit() {
			this.invokeAndStore('init', 'init', this.buildInitPayload())
		},
		runReleaseHandle() {
			this.invokeAndStore('releaseHandle', 'releaseHandle')
		},
		runInitAuthorizeSDK() {
			this.invokeAndStore('initAuthorizeSDK', 'initAuthorizeSDK', { debuggable: true })
		},
		runGetAuthorizeSDKVersion() {
			if (!this.checkRuntime()) return
			try {
				this.setResult('getAuthorizeSDKVersion', sunmiFace.getAuthorizeSDKVersion())
			} catch (e) {
				this.setResult('getAuthorizeSDKVersion 失败', { error: e.message || String(e) })
			}
		},
		runSyncGetAuthorizeCode() {
			this.invokeAndStore('syncGetAuthorizeCode', 'syncGetAuthorizeCode', this.buildAuthorizePayload())
		},
		runAsyncGetAuthorizeToken() {
			if (!this.checkRuntime()) return
			try {
				const payload = this.buildAuthorizePayload()
				const res = sunmiFace.asyncGetAuthorizeToken(payload, (callbackRes) => {
					this.setResult('asyncGetAuthorizeToken callback', { request: payload, response: callbackRes })
				})
				this.setResult('asyncGetAuthorizeToken', { request: payload, response: res })
			} catch (e) {
				this.setResult('asyncGetAuthorizeToken 失败', { error: e.message || String(e) })
			}
		},
		runActivateByLicensePath() {
			this.invokeAndStore('activateByLicensePath', 'activateByLicensePath', {
				licensePath: this.form.licensePath
			})
		},
		runActivateByAppId() {
			this.invokeAndStore('activateByAppId', 'activateByAppId', this.buildAuthorizePayload())
		},
		runStartFaceDetect() {
			const payload = this.buildNativeDetectPayload()
			this.faceEventMessage = '正在打开人脸识别悬浮框...'
			this.invokeAndStream('startFaceDetect', 'startFaceDetect', payload)
		},
		runStopFaceDetect() {
			if (!this.checkRuntime()) return
			try {
				const res = sunmiFace.stopFaceDetect()
				this.faceEventMessage = '已关闭人脸识别'
				this.setResult('stopFaceDetect', res)
			} catch (e) {
				this.setResult('stopFaceDetect 失败', { error: e.message || String(e) })
			}
		},
		runOpenFaceDetect() {
			if (!this.checkRuntime()) return
			try {
				const payload = this.buildNativeDetectPayload()
				this.faceEventMessage = '正在打开 UTS 原生识别界面...'
				this.invokeAndStream('openFaceDetect', 'openFaceDetect', payload)
			} catch (e) {
				this.setResult('openFaceDetect 失败', { error: e.message || String(e) })
			}
		},
		runStartFaceRecognize() {
			const payload = this.buildNativeDetectPayload()
			this.faceEventMessage = '正在打开 startFaceRecognize 兼容入口...'
			this.invokeAndStream('startFaceRecognize', 'startFaceRecognize', payload)
		},
		applyCashierPreset() {
			Object.assign(this.form, {
				floatingWindowMode: true,
				containerBackgroundColor: 'transparent',
				windowWidthRatio: '0.58',
				windowHeightRatio: '0.42',
				windowOffsetXRatio: '0',
				windowOffsetYRatio: '-0.06',
				showCancelButton: false,
				showSwitchCameraButton: false,
				showStatusText: false,
				showStartButton: false,
				autoStartAnalyze: true,
				autoStopOnRecognize: true,
				maxRecognizeFailures: 3,
				showCircleGuide: false,
				showSquareGuide: true,
				showRedLineGuide: false,
				showGuideMask: false,
				guideBoxWidthRatio: '0.62',
				guideBoxHeightRatio: '0.62',
				guideOffsetXRatio: '0',
				guideOffsetYRatio: '0',
				enableSystemFaceDetection: false
			})
			this.faceEventMessage = '已切到收银支付模式'
		},
		applyDebugPreset() {
			Object.assign(this.form, {
				floatingWindowMode: true,
				containerBackgroundColor: 'transparent',
				windowWidthRatio: '0.58',
				windowHeightRatio: '0.42',
				windowOffsetXRatio: '0',
				windowOffsetYRatio: '-0.06',
				showCancelButton: true,
				showSwitchCameraButton: true,
				showStatusText: true,
				showStartButton: true,
				autoStartAnalyze: true,
				autoStopOnRecognize: true,
				maxRecognizeFailures: 0,
				showCircleGuide: false,
				showSquareGuide: true,
				showRedLineGuide: false,
				showGuideMask: false,
				guideBoxWidthRatio: '0.62',
				guideBoxHeightRatio: '0.62',
				guideOffsetXRatio: '0',
				guideOffsetYRatio: '0'
			})
			this.faceEventMessage = '已切到调试模式'
		},
		runClearLocalToken() {
			this.invokeAndStore('clearLocalToken', 'clearLocalToken')
		},
		runInitDB() {
			const payload = {}
			if (this.form.dbPath) {
				payload.dbPath = this.form.dbPath
			}
			this.invokeAndStore('initDB', 'initDB', payload)
		},
		async runGetImageFeatures(type = 'a') {
			const title = type === 'b' ? 'getImageFeatures B' : 'getImageFeatures A'
			const res = await this.invokeAndStore(title, 'getImageFeatures', this.buildImagePayload(type))
			this.syncFeatureState(type, res)
		},
		runReleaseImageFeatures(type = 'a') {
			const target = type === 'b' ? this.imageB : this.imageA
			if (!target.token) {
				uni.showToast({ title: '当前没有 token', icon: 'none' })
				return
			}
			const title = type === 'b' ? 'releaseImageFeatures B' : 'releaseImageFeatures A'
			this.invokeAndStore(title, 'releaseImageFeatures', {
				token: target.token
			})
			target.token = ''
			target.feature = null
		},
		runAddDBRecord() {
			const resolvedImagePath = this.imageA.imagePath || this.imageA.preview || this.form.dbRecord.photoPath
			if (!resolvedImagePath && !(this.imageA.feature && this.imageA.feature.length) && !this.imageA.token) {
				uni.showToast({ title: '请先选择照片或提取特征', icon: 'none' })
				this.setResult('addDBRecord 失败', {
					error: 'imagePath / feature / token 均为空'
				})
				return
			}
			const payload = {
				dbPath: this.form.dbPath,
				id: this.form.dbRecord.id,
				name: this.form.dbRecord.name,
				phone: this.form.dbRecord.phone,
				imgId: this.form.dbRecord.imgId,
				photoPath: this.form.dbRecord.photoPath || resolvedImagePath,
				imagePath: resolvedImagePath,
				...this.buildFeaturePayload('a')
			}
			this.invokeAndStore('addDBRecord', 'addDBRecord', payload).then((res) => {
				const data = res && res.data ? res.data : null
				this.clearImageToken('a')
				if (data && data.imgId) {
					this.form.dbRecord.imgId = data.imgId
				}
			})
		},
		runSearchDB() {
			const payload = this.buildFeaturePayload('a')
			this.invokeAndStore('searchDB', 'searchDB', payload).then(() => {
				if (payload && payload.token) {
					this.clearImageToken('a')
				}
			})
		},
		runGetAllDBRecords() {
			const payload = {}
			if (this.form.dbPath) {
				payload.dbPath = this.form.dbPath
			}
			this.invokeAndStore('getAllDBRecords', 'getAllDBRecords', payload)
		},
		runFaceDetect() {
			this.invokeAndStore('人脸检测', 'getImageFeatures', this.buildImagePayload('a', {
				predictMode: 1,
				livenessMode: 0,
				qualityMode: 0
			})).then((res) => {
				this.syncFeatureState('a', res)
			})
		},
		runFaceRecognition() {
			const payload = this.buildFeaturePayload('a')
			this.invokeAndStore('人脸识别', 'searchDB', payload).then(() => {
				if (payload && payload.token) {
					this.clearImageToken('a')
				}
			})
		},
		runIdCompare() {
			this.invokeAndStore('人证比对', 'compare1v1', {
				first: this.buildFeaturePayload('a'),
				second: this.buildFeaturePayload('b')
			})
		},
		runFaceAttribute() {
			this.invokeAndStore('人脸属性预测', 'getImageFeatures', this.buildImagePayload('a', {
				predictMode: 5,
				livenessMode: 0,
				qualityMode: 0
			}))
		},
		runRgbLiveness() {
			this.invokeAndStore('RGB活体检测', 'getImageFeatures', this.buildImagePayload('a', {
				predictMode: 3,
				livenessMode: 1,
				qualityMode: 0
			}))
		},
		runQualityCheck() {
			this.invokeAndStore('人脸质量评估', 'getImageFeatures', this.buildImagePayload('a', {
				predictMode: 3,
				livenessMode: 0,
				qualityMode: 3
			}))
		},
		runDeleteDBRecord() {
			this.invokeAndStore('deleteDBRecord', 'deleteDBRecord', {
				dbPath: this.form.dbPath,
				id: this.form.dbRecord.id,
				imgId: this.form.dbRecord.imgId
			})
		},
		runClearFaceDatabase() {
			const payload = {}
			if (this.form.dbPath) {
				payload.dbPath = this.form.dbPath
			}
			this.invokeAndStore('clearFaceDatabase', 'clearFaceDatabase', payload)
		},
		chooseImage(type = 'a') {
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					const path = res.tempFilePaths && res.tempFilePaths.length ? res.tempFilePaths[0] : ''
					if (!path) {
						this.setResult('选择图片失败', { error: '未获取到图片路径' })
						return
					}
					uni.saveFile({
						tempFilePath: path,
						success: (saveRes) => {
							const finalPath = saveRes.savedFilePath || path
							const target = type === 'b' ? this.imageB : this.imageA
							target.imagePath = finalPath
							target.preview = finalPath
							this.clearImageState(type, { keepPreview: true })
							target.imagePath = finalPath
							target.preview = finalPath
							this.setResult(type === 'b' ? '选择图片 B' : '选择图片 A', { imagePath: finalPath })
						},
						fail: (err) => {
							this.setResult('保存图片失败', {
								tempFilePath: path,
								error: err.errMsg || JSON.stringify(err)
							})
						}
					})
				}
			})
		}
	}
}
</script>

<style scoped>
.page {
	background: #f5f7fb;
	min-height: 100vh;
}

.scroll {
	padding-bottom: 24rpx;
}

.card {
	margin: 20rpx 24rpx 0;
	padding: 24rpx;
	background: #ffffff;
	border-radius: 24rpx;
	box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.08);
}

.hero {
	margin-top: 24rpx;
	background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
	color: #ffffff;
}

.title {
	font-size: 36rpx;
	font-weight: 700;
}

.desc {
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.6;
	color: rgba(255, 255, 255, 0.88);
}

.guide {
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.7;
	color: rgba(255, 255, 255, 0.92);
}

.section-title {
	font-size: 30rpx;
	font-weight: 700;
	color: #0f172a;
	margin-bottom: 20rpx;
}

.section-tip {
	margin-bottom: 20rpx;
	font-size: 24rpx;
	line-height: 1.6;
	color: #64748b;
}

.field {
	margin-bottom: 20rpx;
}

.label {
	font-size: 24rpx;
	color: #334155;
	margin-bottom: 10rpx;
}

.easy-input {
	width: 100%;
}

.two-col {
	display: flex;
	gap: 16rpx;
}

.col {
	flex: 1;
}

.button-row {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.btn {
	margin: 0;
	background: #e2e8f0;
	color: #0f172a;
	border-radius: 999rpx;
	padding: 0 20rpx;
}

.btn::after {
	border: none;
}

.option {
	background: #edf2f7;
	color: #0f172a;
}

.active {
	background: #0f766e;
	color: #ffffff;
}

.primary {
	background: #2563eb;
	color: #ffffff;
}

.warn {
	background: #ef4444;
	color: #ffffff;
}

.preview-wrap {
	margin: 16rpx 0;
	padding: 16rpx;
	background: #f8fafc;
	border-radius: 18rpx;
}

.camera-embed-wrap {
	margin: 16rpx 0;
	border-radius: 18rpx;
	overflow: hidden;
	background: #000;
}

.camera-embed {
	width: 100%;
	height: 720rpx;
}

.preview {
	width: 100%;
	height: 320rpx;
	border-radius: 12rpx;
	background: #e2e8f0;
}

.result-box {
	background: #0f172a;
	color: #dbeafe;
	border-radius: 18rpx;
	padding: 20rpx;
	font-size: 24rpx;
	line-height: 1.6;
	word-break: break-all;
	white-space: pre-wrap;
}
</style>
