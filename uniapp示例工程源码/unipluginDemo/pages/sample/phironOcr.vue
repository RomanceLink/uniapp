<template>
	<view class="page">
		<view class="card hero">
			<view class="title">Phiron OCR 示例</view>
			<view class="desc">面向电子称拍照识别的 UTS 插件示例，支持 OpenCV 预处理和 OCR 结果查看。</view>
		</view>

		<view class="card">
			<view class="label">图片路径</view>
			<uni-easyinput v-model="form.imagePath" placeholder="/storage/emulated/0/DCIM/scale.jpg" />
			<view class="tip">请先把电子称照片放到设备上，再填绝对路径。</view>
		</view>

		<view class="card">
			<view class="section-title">电子称预设</view>
			<view class="switch-row">
				<text>灰度化</text>
				<switch :checked="form.preprocess.enableGray" @change="onSwitch('enableGray', $event)" color="#2563eb" />
			</view>
			<view class="switch-row">
				<text>降噪</text>
				<switch :checked="form.preprocess.enableDenoise" @change="onSwitch('enableDenoise', $event)" color="#2563eb" />
			</view>
			<view class="switch-row">
				<text>锐化</text>
				<switch :checked="form.preprocess.enableSharpen" @change="onSwitch('enableSharpen', $event)" color="#2563eb" />
			</view>
			<view class="switch-row">
				<text>对比度增强</text>
				<switch :checked="form.preprocess.enableContrast" @change="onSwitch('enableContrast', $event)" color="#2563eb" />
			</view>
			<view class="switch-row">
				<text>自适应二值化</text>
				<switch :checked="form.preprocess.enableAdaptiveThreshold" @change="onSwitch('enableAdaptiveThreshold', $event)" color="#2563eb" />
			</view>
			<view class="switch-row">
				<text>旋转矫正</text>
				<switch :checked="form.preprocess.enableDeskew" @change="onSwitch('enableDeskew', $event)" color="#2563eb" />
			</view>
		</view>

		<view class="card">
			<view class="button-row">
				<button class="btn" size="mini" @click="runCheckEnvironment">检查环境</button>
				<button class="btn" size="mini" @click="runPreprocess">仅预处理</button>
				<button class="btn primary" size="mini" @click="runRecognizeScale">识别电子称数值</button>
				<button class="btn" size="mini" @click="runRecognizeAll">通用 OCR</button>
			</view>
		</view>

		<view class="card">
			<view class="label">结果</view>
			<scroll-view scroll-y class="result-box">
				<text selectable>{{ resultText }}</text>
			</scroll-view>
		</view>
	</view>
</template>

<script>
import * as PhironOcr from '@/uni_modules/phiron-ocr'

export default {
	data() {
		return {
			form: {
				imagePath: '',
				preprocess: {
					enableGray: true,
					enableDenoise: true,
					enableSharpen: true,
					enableContrast: true,
					enableAdaptiveThreshold: true,
					enableDeskew: true
				}
			},
			resultText: '等待执行...'
		}
	},
	methods: {
		onSwitch(key, event) {
			this.form.preprocess[key] = !!event.detail.value
		},
		runCheckEnvironment() {
			this.resultText = JSON.stringify(PhironOcr.checkEnvironment(), null, 2)
		},
		runPreprocess() {
			const outputPath = `${plus.io.convertLocalFileSystemURL('_doc')}/phiron-ocr-preprocess.png`
			const result = PhironOcr.preprocessImage({
				imagePath: this.form.imagePath,
				outputPath,
				...this.form.preprocess
			})
			this.resultText = JSON.stringify(result, null, 2)
		},
		runRecognizeScale() {
			const result = PhironOcr.recognizeScaleValue({
				imagePath: this.form.imagePath,
				includeRawBlocks: true,
				preprocess: this.form.preprocess
			})
			this.resultText = JSON.stringify(result, null, 2)
		},
		runRecognizeAll() {
			const result = PhironOcr.recognize({
				imagePath: this.form.imagePath,
				preferDigits: true,
				extractBestNumericCandidate: true,
				includeRawBlocks: true,
				allowedChars: '0123456789.-kgKG',
				expectedRegex: '-?\\d+(?:\\.\\d+)?',
				preprocess: this.form.preprocess
			})
			this.resultText = JSON.stringify(result, null, 2)
		}
	}
}
</script>

<style>
.page {
	padding: 24rpx;
	background: #f4f7fb;
	min-height: 100vh;
}

.card {
	background: #ffffff;
	border-radius: 20rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
	box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
}

.hero {
	background: linear-gradient(135deg, #dbeafe, #eff6ff);
}

.title {
	font-size: 36rpx;
	font-weight: 700;
	color: #0f172a;
}

.desc, .tip {
	margin-top: 12rpx;
	font-size: 26rpx;
	color: #475569;
}

.section-title, .label {
	font-size: 28rpx;
	font-weight: 600;
	color: #1e293b;
	margin-bottom: 16rpx;
}

.switch-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16rpx 0;
	border-bottom: 1rpx solid #e2e8f0;
}

.button-row {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.btn {
	background: #e2e8f0;
	color: #0f172a;
}

.btn.primary {
	background: #2563eb;
	color: #ffffff;
}

.result-box {
	max-height: 680rpx;
	background: #0f172a;
	color: #e2e8f0;
	border-radius: 16rpx;
	padding: 20rpx;
	box-sizing: border-box;
}
</style>
