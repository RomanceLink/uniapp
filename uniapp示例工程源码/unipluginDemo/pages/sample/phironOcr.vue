<template>
	<view class="page">
		<view class="card hero">
			<view class="title">Phiron OCR 示例</view>
			<view class="desc">面向电子称拍照识别的 UTS 插件示例，支持 OpenCV 预处理和 OCR 结果查看。</view>
		</view>

		<view class="card">
			<view class="label">图片来源</view>
			<view class="button-row">
				<button class="btn" size="mini" @click="requestImagePermissions">申请图片权限</button>
				<button class="btn primary" size="mini" @click="pickFromGallery">从图片库选择</button>
				<button class="btn" size="mini" @click="pickFromFile">从文件选择</button>
			</view>
			<view class="tip">选完图片后会自动开始识别，不需要手填路径。</view>
			<view class="path-box">
				<text selectable>{{ selectedPath || '尚未选择图片' }}</text>
			</view>
			<uni-easyinput v-model="form.imagePath" placeholder="也可以手动填路径或 content:// URI" />
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
				imageUri: '',
				base64: '',
				preprocess: {
					enableGray: true,
					enableDenoise: true,
					enableSharpen: true,
					enableContrast: true,
					enableAdaptiveThreshold: true,
					enableDeskew: true
				}
			},
			resultText: '等待执行...',
			selectedPath: '',
			isRecognizing: false
		}
	},
	methods: {
		onSwitch(key, event) {
			this.form.preprocess[key] = !!event.detail.value
		},
		getRecognizePayload() {
			return {
				imagePath: this.form.imagePath || undefined,
				imageUri: this.form.imageUri || undefined,
				base64: this.form.base64 || undefined,
				preprocess: this.form.preprocess
			}
		},
		applySelectedImage(path, uri = '', base64 = '') {
			this.form.imagePath = path || ''
			this.form.imageUri = uri || ''
			this.form.base64 = base64 || ''
			this.selectedPath = uri || path || (base64 ? 'base64-image' : '')
		},
		requestImagePermissions() {
			// #ifdef APP-PLUS
			const permissions = ['android.permission.READ_EXTERNAL_STORAGE']
			if (plus.os.name === 'Android') {
				if (plus.android.invoke('android.os.Build$VERSION', 'SDK_INT') >= 33) {
					permissions.push('android.permission.READ_MEDIA_IMAGES')
				}
				if (plus.android.invoke('android.os.Build$VERSION', 'SDK_INT') >= 34) {
					permissions.push('android.permission.READ_MEDIA_VISUAL_USER_SELECTED')
				}
				plus.android.requestPermissions(
					permissions,
					(result) => {
						this.resultText = JSON.stringify({
							success: true,
							code: 0,
							message: '权限申请完成',
							data: result
						}, null, 2)
					},
					(error) => {
						this.resultText = JSON.stringify({
							success: false,
							code: -1,
							message: '权限申请失败',
							data: error
						}, null, 2)
					}
				)
			}
			// #endif
		},
		pickFromGallery() {
			// #ifdef APP-PLUS
			this.resultText = '正在打开图片库...'
			plus.gallery.pick(
				(event) => {
					const path = typeof event === 'string' ? event : (event.files && event.files[0]) || ''
					this.applySelectedImage(path)
					this.resultText = `已选择图片，开始识别：${path}`
					setTimeout(() => {
						this.runRecognizeScale()
					}, 80)
				},
				(error) => {
					this.resultText = JSON.stringify({
						success: false,
						code: -1,
						message: '选择图片失败',
						data: error
					}, null, 2)
				},
				{
					filter: 'image',
					multiple: false,
					system: true
				}
			)
			// #endif
		},
		pickFromFile() {
			const input = document.createElement('input')
			input.type = 'file'
			input.accept = 'image/*'
			input.style.position = 'fixed'
			input.style.left = '-9999px'
			document.body.appendChild(input)
			input.onchange = () => {
				const file = input.files && input.files[0]
				if (!file) {
					document.body.removeChild(input)
					this.resultText = '未选择文件'
					return
				}
				this.resultText = `已选择文件，正在读取：${file.name}`
				const reader = new FileReader()
				reader.onload = () => {
					const base64 = typeof reader.result === 'string' ? reader.result : ''
					this.applySelectedImage(file.name, '', base64)
					this.resultText = `文件读取完成，开始识别：${file.name}`
					setTimeout(() => {
						this.runRecognizeScale()
						document.body.removeChild(input)
					}, 80)
				}
				reader.onerror = (error) => {
					this.resultText = JSON.stringify({
						success: false,
						code: -1,
						message: '文件读取失败',
						data: error
					}, null, 2)
					document.body.removeChild(input)
				}
				reader.readAsDataURL(file)
			}
			input.click()
		},
		runCheckEnvironment() {
			this.resultText = JSON.stringify(PhironOcr.checkEnvironment(), null, 2)
		},
		runPreprocess() {
			// #ifdef APP-PLUS
			const outputPath = `${plus.io.convertLocalFileSystemURL('_doc')}/phiron-ocr-preprocess.png`
			// #endif
			try {
				const result = PhironOcr.preprocessImage({
					imagePath: this.form.imagePath || this.form.imageUri,
					base64: this.form.base64 || undefined,
					outputPath,
					...this.form.preprocess
				})
				this.resultText = JSON.stringify(result, null, 2)
			} catch (error) {
				this.resultText = JSON.stringify({
					success: false,
					code: -1,
					message: '预处理失败',
					data: String(error)
				}, null, 2)
			}
		},
		runRecognizeScale() {
			if (this.isRecognizing) {
				return
			}
			if (!this.form.imagePath && !this.form.imageUri && !this.form.base64) {
				this.resultText = '请先选择图片或文件'
				return
			}
			this.isRecognizing = true
			this.resultText = '识别中，请稍候...'
			setTimeout(() => {
				try {
					const result = PhironOcr.recognizeScaleValue({
						...this.getRecognizePayload(),
						includeRawBlocks: true,
					})
					this.resultText = JSON.stringify(result, null, 2)
				} catch (error) {
					this.resultText = JSON.stringify({
						success: false,
						code: -1,
						message: '电子称识别失败',
						data: String(error)
					}, null, 2)
				} finally {
					this.isRecognizing = false
				}
			}, 80)
		},
		runRecognizeAll() {
			try {
				const result = PhironOcr.recognize({
					...this.getRecognizePayload(),
					preferDigits: true,
					extractBestNumericCandidate: true,
					includeRawBlocks: true,
					allowedChars: '0123456789.-kgKG',
					expectedRegex: '-?\\d+(?:\\.\\d+)?'
				})
				this.resultText = JSON.stringify(result, null, 2)
			} catch (error) {
				this.resultText = JSON.stringify({
					success: false,
					code: -1,
					message: '通用 OCR 识别失败',
					data: String(error)
				}, null, 2)
			}
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

.path-box {
	margin-top: 16rpx;
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
	background: #eff6ff;
	color: #1e3a8a;
	word-break: break-all;
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
