@file:Suppress(
    "UNCHECKED_CAST",
    "USELESS_CAST",
    "INAPPLICABLE_JVM_NAME",
    "UNUSED_ANONYMOUS_PARAMETER",
    "SENSELESS_COMPARISON",
    "NAME_SHADOWING",
    "UNNECESSARY_NOT_NULL_ASSERTION"
)

package uts.sdk.modules.phironSunmiFace

import io.dcloud.uts.JSON
import io.dcloud.uts.UTSArray
import io.dcloud.uts.UTSCallback
import io.dcloud.uts.UTSJSONObject
import io.dcloud.uts.UTSObject
import uts.sdk.modules.phironsunmiFace.SunmiFaceNative
import uts.sdk.modules.phironsunmiFace.SunmiFaceRealtimeNative

typealias SunmiFaceCommonResult = Any

open class SunmiFaceAuthorizeOptions(
    open var appId: String? = null,
    open var forceRefresh: Boolean? = null,
) : UTSObject()

open class SunmiFaceLicenseOptions(
    open var licensePath: String,
) : UTSObject()

open class SunmiFaceInitOptions(
    open var useAssetConfig: Boolean? = null,
    open var configPath: String? = null,
    open var dbPath: String? = null,
) : UTSObject()

open class SunmiFaceConfigOptions(
    open var threadNum: Number? = null,
    open var distanceThreshold: Number? = null,
    open var faceScoreThreshold: Number? = null,
    open var minFaceSize: Number? = null,
    open var depthXOffset: Number? = null,
    open var depthYOffset: Number? = null,
    open var boxSortMode: Number? = null,
) : UTSObject()

open class SunmiFaceImageOptions(
    open var imagePath: String? = null,
    open var base64: String? = null,
    open var token: String? = null,
    open var feature: UTSArray<Number>? = null,
    open var keepAlive: Boolean? = null,
    open var retainToken: Boolean? = null,
    open var maxFaceCount: Number? = null,
    open var predictMode: Number? = null,
    open var livenessMode: Number? = null,
    open var qualityMode: Number? = null,
    open var decodeMaxSize: Number? = null,
    open var rectRotation: Number? = null,
    open var rectMirrorX: Boolean? = null,
    open var distanceThreshold: Number? = null,
    open var minFaceSize: Number? = null,
) : UTSObject()

open class SunmiFaceDBRecordOptions(
    open var imagePath: String? = null,
    open var base64: String? = null,
    open var token: String? = null,
    open var feature: UTSArray<Number>? = null,
    open var keepAlive: Boolean? = null,
    open var retainToken: Boolean? = null,
    open var maxFaceCount: Number? = null,
    open var predictMode: Number? = null,
    open var livenessMode: Number? = null,
    open var qualityMode: Number? = null,
    open var decodeMaxSize: Number? = null,
    open var rectRotation: Number? = null,
    open var rectMirrorX: Boolean? = null,
    open var distanceThreshold: Number? = null,
    open var minFaceSize: Number? = null,
    open var dbPath: String? = null,
    open var id: String? = null,
    open var name: String? = null,
    open var phone: String? = null,
    open var imgId: String? = null,
    open var photoPath: String? = null,
) : UTSObject()

open class SunmiFaceCompareOptions(
    open var first: SunmiFaceImageOptions,
    open var second: SunmiFaceImageOptions,
) : UTSObject()

open class SunmiFaceDetectOptions(
    open var appId: String? = null,
    open var licensePath: String? = null,
    open var forceRefresh: Boolean? = null,
    open var dbPath: String? = null,
    open var cameraFacing: String? = null,
    open var showCloseButton: Boolean? = null,
    open var showStartButton: Boolean? = null,
    open var showStatusText: Boolean? = null,
    open var autoStartAnalyze: Boolean? = null,
    open var autoStopOnRecognize: Boolean? = null,
    open var maxRecognizeFailures: Number? = null,
    open var predictMode: Number? = null,
    open var livenessMode: Number? = null,
    open var qualityMode: Number? = null,
    open var maxFaceCount: Number? = null,
    open var faceScoreThreshold: Number? = null,
    open var threadNum: Number? = null,
    open var analyzeIntervalMs: Number? = null,
    open var previewDecodeMaxSize: Number? = null,
    open var displayOrientationDeg: Number? = null,
    open var captureImageRotationDeg: Number? = null,
    open var captureMirrorX: Boolean? = null,
    open var rectRotation: Number? = null,
    open var rectMirrorX: Boolean? = null,
    open var showSquareGuide: Boolean? = null,
    open var showGuideMask: Boolean? = null,
    open var guideBoxWidthRatio: Number? = null,
    open var guideBoxHeightRatio: Number? = null,
    open var guideOffsetXRatio: Number? = null,
    open var guideOffsetYRatio: Number? = null,
    open var floatingWindowMode: Boolean? = null,
    open var containerBackgroundColor: String? = null,
    open var windowWidthRatio: Number? = null,
    open var windowHeightRatio: Number? = null,
    open var windowOffsetXRatio: Number? = null,
    open var windowOffsetYRatio: Number? = null,
) : UTSObject()

typealias SunmiFaceCallback = (res: SunmiFaceCommonResult) -> Unit
typealias SunmiFaceRealtimeCallback = (res: SunmiFaceCommonResult) -> Unit

fun parseResult(resultJson: String): SunmiFaceCommonResult {
    return JSON.parse(resultJson) as Any
}

fun stringifyOptions(options: Any?): String? {
    return if (options == null) null else JSON.stringify(options)
}

fun getVersion(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getVersionJson())
fun getDeviceInfo(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getDeviceInfoJson())
fun initAuthorizeSDK(options: Any? = null): SunmiFaceCommonResult = parseResult(SunmiFaceNative.initAuthorizeSDKJson(stringifyOptions(options)))
fun getAuthorizeSDKVersion(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getAuthorizeSDKVersionJson())
fun syncGetAuthorizeCode(options: SunmiFaceAuthorizeOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.syncGetAuthorizeCodeJson(stringifyOptions(options)))

fun asyncGetAuthorizeToken(options: SunmiFaceAuthorizeOptions, callback: SunmiFaceCallback? = null): SunmiFaceCommonResult {
    val result = parseResult(SunmiFaceNative.syncGetAuthorizeCodeJson(stringifyOptions(options)))
    callback?.invoke(result)
    return result
}

fun clearLocalToken(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.clearLocalTokenJson())
fun createHandle(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.createHandleJson())
fun init(options: SunmiFaceInitOptions? = null): SunmiFaceCommonResult = parseResult(SunmiFaceNative.initJson(stringifyOptions(options)))
fun activateByLicensePath(options: SunmiFaceLicenseOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.activateByLicensePathJson(stringifyOptions(options)))
fun activateByAppId(options: SunmiFaceAuthorizeOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.activateByAppIdJson(stringifyOptions(options)))
fun verifyLicense(options: Any): SunmiFaceCommonResult = parseResult(SunmiFaceNative.verifyLicenseJson(stringifyOptions(options)))
fun getErrorString(options: Any): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getErrorStringJson(stringifyOptions(options)))
fun setConfig(options: SunmiFaceConfigOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.setConfigJson(stringifyOptions(options)))
fun getConfig(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getConfigJson())
fun initDB(options: Any? = null): SunmiFaceCommonResult = parseResult(SunmiFaceNative.initDBJson(stringifyOptions(options)))
fun getImageFeatures(options: SunmiFaceImageOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getImageFeaturesJson(stringifyOptions(options)))
fun releaseImageFeatures(options: Any): SunmiFaceCommonResult = parseResult(SunmiFaceNative.releaseImageFeaturesJson(stringifyOptions(options)))
fun addDBRecord(options: SunmiFaceDBRecordOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.addDBRecordJson(stringifyOptions(options)))
fun searchDB(options: Any): SunmiFaceCommonResult = parseResult(SunmiFaceNative.searchDBJson(stringifyOptions(options)))
fun compare1v1(options: SunmiFaceCompareOptions): SunmiFaceCommonResult = parseResult(SunmiFaceNative.compare1v1Json(stringifyOptions(options)))
fun deleteDBRecord(options: Any): SunmiFaceCommonResult = parseResult(SunmiFaceNative.deleteDBRecordJson(stringifyOptions(options)))
fun getAllDBRecords(options: Any? = null): SunmiFaceCommonResult = parseResult(SunmiFaceNative.getAllDBRecordsJson(stringifyOptions(options)))
fun clearFaceDatabase(options: Any? = null): SunmiFaceCommonResult = parseResult(SunmiFaceNative.clearFaceDatabaseJson(stringifyOptions(options)))
fun releaseHandle(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.releaseHandleJson())
fun checkPermissions(): SunmiFaceCommonResult = parseResult(SunmiFaceNative.checkPermissionsJson())

fun wrapRealtimeCallback(callback: SunmiFaceRealtimeCallback?): ((String) -> Unit)? {
    if (callback == null) return null
    return fun(resultJson: String) {
        callback(parseResult(resultJson))
    }
}

fun startFaceRecognize(options: SunmiFaceDetectOptions, callback: SunmiFaceRealtimeCallback? = null): SunmiFaceCommonResult =
    parseResult(SunmiFaceRealtimeNative.startFaceRecognizeJson(stringifyOptions(options), wrapRealtimeCallback(callback)))

fun startFaceDetect(options: SunmiFaceDetectOptions, callback: SunmiFaceRealtimeCallback? = null): SunmiFaceCommonResult =
    parseResult(SunmiFaceRealtimeNative.startFaceDetectJson(stringifyOptions(options), wrapRealtimeCallback(callback)))

fun stopFaceDetect(): SunmiFaceCommonResult = parseResult(SunmiFaceRealtimeNative.stopFaceDetectJson())

fun openFaceDetect(options: SunmiFaceDetectOptions, callback: SunmiFaceRealtimeCallback? = null): SunmiFaceCommonResult =
    parseResult(SunmiFaceRealtimeNative.openFaceDetectJson(stringifyOptions(options), wrapRealtimeCallback(callback)))

open class SunmiFaceAuthorizeOptionsJSONObject : UTSJSONObject() {
    open var appId: String? = null
    open var forceRefresh: Boolean? = null
}

open class SunmiFaceLicenseOptionsJSONObject : UTSJSONObject() {
    open lateinit var licensePath: String
}

open class SunmiFaceInitOptionsJSONObject : UTSJSONObject() {
    open var useAssetConfig: Boolean? = null
    open var configPath: String? = null
    open var dbPath: String? = null
}

open class SunmiFaceConfigOptionsJSONObject : UTSJSONObject() {
    open var threadNum: Number? = null
    open var distanceThreshold: Number? = null
    open var faceScoreThreshold: Number? = null
    open var minFaceSize: Number? = null
    open var depthXOffset: Number? = null
    open var depthYOffset: Number? = null
    open var boxSortMode: Number? = null
}

open class SunmiFaceImageOptionsJSONObject : UTSJSONObject() {
    open var imagePath: String? = null
    open var base64: String? = null
    open var token: String? = null
    open var feature: UTSArray<Number>? = null
    open var keepAlive: Boolean? = null
    open var retainToken: Boolean? = null
    open var maxFaceCount: Number? = null
    open var predictMode: Number? = null
    open var livenessMode: Number? = null
    open var qualityMode: Number? = null
    open var decodeMaxSize: Number? = null
    open var rectRotation: Number? = null
    open var rectMirrorX: Boolean? = null
    open var distanceThreshold: Number? = null
    open var minFaceSize: Number? = null
}

open class SunmiFaceDBRecordOptionsJSONObject : UTSJSONObject() {
    open var imagePath: String? = null
    open var base64: String? = null
    open var token: String? = null
    open var feature: UTSArray<Number>? = null
    open var keepAlive: Boolean? = null
    open var retainToken: Boolean? = null
    open var maxFaceCount: Number? = null
    open var predictMode: Number? = null
    open var livenessMode: Number? = null
    open var qualityMode: Number? = null
    open var decodeMaxSize: Number? = null
    open var rectRotation: Number? = null
    open var rectMirrorX: Boolean? = null
    open var distanceThreshold: Number? = null
    open var minFaceSize: Number? = null
    open var dbPath: String? = null
    open var id: String? = null
    open var name: String? = null
    open var phone: String? = null
    open var imgId: String? = null
    open var photoPath: String? = null
}

open class SunmiFaceCompareOptionsJSONObject : UTSJSONObject() {
    open lateinit var first: SunmiFaceImageOptions
    open lateinit var second: SunmiFaceImageOptions
}

open class SunmiFaceDetectOptionsJSONObject : UTSJSONObject() {
    open var appId: String? = null
    open var licensePath: String? = null
    open var forceRefresh: Boolean? = null
    open var dbPath: String? = null
    open var cameraFacing: String? = null
    open var showCloseButton: Boolean? = null
    open var showStartButton: Boolean? = null
    open var showStatusText: Boolean? = null
    open var autoStartAnalyze: Boolean? = null
    open var autoStopOnRecognize: Boolean? = null
    open var maxRecognizeFailures: Number? = null
    open var predictMode: Number? = null
    open var livenessMode: Number? = null
    open var qualityMode: Number? = null
    open var maxFaceCount: Number? = null
    open var faceScoreThreshold: Number? = null
    open var threadNum: Number? = null
    open var analyzeIntervalMs: Number? = null
    open var previewDecodeMaxSize: Number? = null
    open var displayOrientationDeg: Number? = null
    open var captureImageRotationDeg: Number? = null
    open var captureMirrorX: Boolean? = null
    open var rectRotation: Number? = null
    open var rectMirrorX: Boolean? = null
    open var showSquareGuide: Boolean? = null
    open var showGuideMask: Boolean? = null
    open var guideBoxWidthRatio: Number? = null
    open var guideBoxHeightRatio: Number? = null
    open var guideOffsetXRatio: Number? = null
    open var guideOffsetYRatio: Number? = null
    open var floatingWindowMode: Boolean? = null
    open var containerBackgroundColor: String? = null
    open var windowWidthRatio: Number? = null
    open var windowHeightRatio: Number? = null
    open var windowOffsetXRatio: Number? = null
    open var windowOffsetYRatio: Number? = null
}

fun getVersionByJs(): SunmiFaceCommonResult = getVersion()
fun getDeviceInfoByJs(): SunmiFaceCommonResult = getDeviceInfo()
fun initAuthorizeSDKByJs(options: Any? = null): SunmiFaceCommonResult = initAuthorizeSDK(options)
fun getAuthorizeSDKVersionByJs(): SunmiFaceCommonResult = getAuthorizeSDKVersion()
fun syncGetAuthorizeCodeByJs(options: SunmiFaceAuthorizeOptionsJSONObject): SunmiFaceCommonResult =
    syncGetAuthorizeCode(SunmiFaceAuthorizeOptions(appId = options.appId, forceRefresh = options.forceRefresh))

fun asyncGetAuthorizeTokenByJs(options: SunmiFaceAuthorizeOptionsJSONObject, callback: UTSCallback? = null): SunmiFaceCommonResult =
    asyncGetAuthorizeToken(
        SunmiFaceAuthorizeOptions(appId = options.appId, forceRefresh = options.forceRefresh),
        callback?.let {
            fun(res: SunmiFaceCommonResult) { it(res) }
        }
    )

fun clearLocalTokenByJs(): SunmiFaceCommonResult = clearLocalToken()
fun createHandleByJs(): SunmiFaceCommonResult = createHandle()
fun initByJs(options: SunmiFaceInitOptionsJSONObject? = null): SunmiFaceCommonResult =
    init(options?.let { SunmiFaceInitOptions(useAssetConfig = it.useAssetConfig, configPath = it.configPath, dbPath = it.dbPath) })

fun activateByLicensePathByJs(options: SunmiFaceLicenseOptionsJSONObject): SunmiFaceCommonResult =
    activateByLicensePath(SunmiFaceLicenseOptions(licensePath = options.licensePath))

fun activateByAppIdByJs(options: SunmiFaceAuthorizeOptionsJSONObject): SunmiFaceCommonResult =
    activateByAppId(SunmiFaceAuthorizeOptions(appId = options.appId, forceRefresh = options.forceRefresh))

fun verifyLicenseByJs(options: Any): SunmiFaceCommonResult = verifyLicense(options)
fun getErrorStringByJs(options: Any): SunmiFaceCommonResult = getErrorString(options)
fun setConfigByJs(options: SunmiFaceConfigOptionsJSONObject): SunmiFaceCommonResult =
    setConfig(
        SunmiFaceConfigOptions(
            threadNum = options.threadNum,
            distanceThreshold = options.distanceThreshold,
            faceScoreThreshold = options.faceScoreThreshold,
            minFaceSize = options.minFaceSize,
            depthXOffset = options.depthXOffset,
            depthYOffset = options.depthYOffset,
            boxSortMode = options.boxSortMode,
        )
    )

fun getConfigByJs(): SunmiFaceCommonResult = getConfig()
fun initDBByJs(options: Any? = null): SunmiFaceCommonResult = initDB(options)
fun getImageFeaturesByJs(options: SunmiFaceImageOptionsJSONObject): SunmiFaceCommonResult =
    getImageFeatures(
        SunmiFaceImageOptions(
            imagePath = options.imagePath,
            base64 = options.base64,
            token = options.token,
            feature = options.feature,
            keepAlive = options.keepAlive,
            retainToken = options.retainToken,
            maxFaceCount = options.maxFaceCount,
            predictMode = options.predictMode,
            livenessMode = options.livenessMode,
            qualityMode = options.qualityMode,
            decodeMaxSize = options.decodeMaxSize,
            rectRotation = options.rectRotation,
            rectMirrorX = options.rectMirrorX,
            distanceThreshold = options.distanceThreshold,
            minFaceSize = options.minFaceSize,
        )
    )

fun releaseImageFeaturesByJs(options: Any): SunmiFaceCommonResult = releaseImageFeatures(options)
fun addDBRecordByJs(options: SunmiFaceDBRecordOptionsJSONObject): SunmiFaceCommonResult =
    addDBRecord(
        SunmiFaceDBRecordOptions(
            imagePath = options.imagePath,
            base64 = options.base64,
            token = options.token,
            feature = options.feature,
            keepAlive = options.keepAlive,
            retainToken = options.retainToken,
            maxFaceCount = options.maxFaceCount,
            predictMode = options.predictMode,
            livenessMode = options.livenessMode,
            qualityMode = options.qualityMode,
            decodeMaxSize = options.decodeMaxSize,
            rectRotation = options.rectRotation,
            rectMirrorX = options.rectMirrorX,
            distanceThreshold = options.distanceThreshold,
            minFaceSize = options.minFaceSize,
            dbPath = options.dbPath,
            id = options.id,
            name = options.name,
            phone = options.phone,
            imgId = options.imgId,
            photoPath = options.photoPath,
        )
    )

fun searchDBByJs(options: Any): SunmiFaceCommonResult = searchDB(options)
fun compare1v1ByJs(options: SunmiFaceCompareOptionsJSONObject): SunmiFaceCommonResult =
    compare1v1(SunmiFaceCompareOptions(first = options.first, second = options.second))

fun deleteDBRecordByJs(options: Any): SunmiFaceCommonResult = deleteDBRecord(options)
fun getAllDBRecordsByJs(options: Any? = null): SunmiFaceCommonResult = getAllDBRecords(options)
fun clearFaceDatabaseByJs(options: Any? = null): SunmiFaceCommonResult = clearFaceDatabase(options)
fun releaseHandleByJs(): SunmiFaceCommonResult = releaseHandle()
fun checkPermissionsByJs(): SunmiFaceCommonResult = checkPermissions()

fun startFaceRecognizeByJs(options: SunmiFaceDetectOptionsJSONObject, callback: UTSCallback? = null): SunmiFaceCommonResult =
    startFaceRecognize(buildDetectOptions(options), callback?.let { fun(res: SunmiFaceCommonResult) { it(res) } })

fun startFaceDetectByJs(options: SunmiFaceDetectOptionsJSONObject, callback: UTSCallback? = null): SunmiFaceCommonResult =
    startFaceDetect(buildDetectOptions(options), callback?.let { fun(res: SunmiFaceCommonResult) { it(res) } })

fun stopFaceDetectByJs(): SunmiFaceCommonResult = stopFaceDetect()

fun openFaceDetectByJs(options: SunmiFaceDetectOptionsJSONObject, callback: UTSCallback? = null): SunmiFaceCommonResult =
    openFaceDetect(buildDetectOptions(options), callback?.let { fun(res: SunmiFaceCommonResult) { it(res) } })

private fun buildDetectOptions(options: SunmiFaceDetectOptionsJSONObject): SunmiFaceDetectOptions {
    return SunmiFaceDetectOptions(
        appId = options.appId,
        licensePath = options.licensePath,
        forceRefresh = options.forceRefresh,
        dbPath = options.dbPath,
        cameraFacing = options.cameraFacing,
        showCloseButton = options.showCloseButton,
        showStartButton = options.showStartButton,
        showStatusText = options.showStatusText,
        autoStartAnalyze = options.autoStartAnalyze,
        autoStopOnRecognize = options.autoStopOnRecognize,
        maxRecognizeFailures = options.maxRecognizeFailures,
        predictMode = options.predictMode,
        livenessMode = options.livenessMode,
        qualityMode = options.qualityMode,
        maxFaceCount = options.maxFaceCount,
        faceScoreThreshold = options.faceScoreThreshold,
        threadNum = options.threadNum,
        analyzeIntervalMs = options.analyzeIntervalMs,
        previewDecodeMaxSize = options.previewDecodeMaxSize,
        displayOrientationDeg = options.displayOrientationDeg,
        captureImageRotationDeg = options.captureImageRotationDeg,
        captureMirrorX = options.captureMirrorX,
        rectRotation = options.rectRotation,
        rectMirrorX = options.rectMirrorX,
        showSquareGuide = options.showSquareGuide,
        showGuideMask = options.showGuideMask,
        guideBoxWidthRatio = options.guideBoxWidthRatio,
        guideBoxHeightRatio = options.guideBoxHeightRatio,
        guideOffsetXRatio = options.guideOffsetXRatio,
        guideOffsetYRatio = options.guideOffsetYRatio,
        floatingWindowMode = options.floatingWindowMode,
        containerBackgroundColor = options.containerBackgroundColor,
        windowWidthRatio = options.windowWidthRatio,
        windowHeightRatio = options.windowHeightRatio,
        windowOffsetXRatio = options.windowOffsetXRatio,
        windowOffsetYRatio = options.windowOffsetYRatio,
    )
}
