@file:Suppress(
    "UNCHECKED_CAST",
    "USELESS_CAST",
    "INAPPLICABLE_JVM_NAME",
    "UNUSED_ANONYMOUS_PARAMETER",
    "SENSELESS_COMPARISON",
    "NAME_SHADOWING",
    "UNNECESSARY_NOT_NULL_ASSERTION"
)

package uts.sdk.modules.phironOcr

import io.dcloud.uts.JSON
import io.dcloud.uts.UTSArray
import io.dcloud.uts.UTSJSONObject
import io.dcloud.uts.UTSObject
import uts.sdk.modules.phironocr.OcrNative

typealias PhironOcrResult = Any

open class PhironOcrQuadPoint(
    open var x: Number,
    open var y: Number,
) : UTSObject()

open class PhironOcrRoi(
    open var left: Number? = null,
    open var top: Number? = null,
    open var width: Number? = null,
    open var height: Number? = null,
) : UTSObject()

open class PhironOcrPreprocessOptions(
    open var enableGray: Boolean? = null,
    open var enableDenoise: Boolean? = null,
    open var enableSharpen: Boolean? = null,
    open var enableContrast: Boolean? = null,
    open var enableAdaptiveThreshold: Boolean? = null,
    open var enableDeskew: Boolean? = null,
    open var enablePerspectiveCorrection: Boolean? = null,
    open var manualRotateDegrees: Number? = null,
    open var cropRoi: PhironOcrRoi? = null,
    open var perspectivePoints: UTSArray<PhironOcrQuadPoint>? = null,
    open var contrastAlpha: Number? = null,
    open var contrastBeta: Number? = null,
    open var adaptiveBlockSize: Number? = null,
    open var adaptiveC: Number? = null,
    open var denoiseKernelSize: Number? = null,
    open var sharpenSigma: Number? = null,
    open var outputPath: String? = null,
    open var returnBase64: Boolean? = null,
) : UTSObject()

open class PhironOcrRecognizeOptions(
    open var imagePath: String? = null,
    open var imageUri: String? = null,
    open var base64: String? = null,
    open var expectedRegex: String? = null,
    open var allowedChars: String? = null,
    open var maxResultCount: Number? = null,
    open var preferDigits: Boolean? = null,
    open var extractBestNumericCandidate: Boolean? = null,
    open var includeRawBlocks: Boolean? = null,
    open var preprocess: PhironOcrPreprocessOptions? = null,
) : UTSObject()

fun parseResult(resultJson: String): PhironOcrResult {
    return JSON.parse(resultJson) as Any
}

fun stringifyOptions(options: Any?): String? {
    return if (options == null) null else JSON.stringify(options)
}

fun getVersion(): PhironOcrResult = parseResult(OcrNative.getVersionJson())
fun checkEnvironment(): PhironOcrResult = parseResult(OcrNative.checkEnvironmentJson())
fun preprocessImage(options: PhironOcrPreprocessOptions): PhironOcrResult = parseResult(OcrNative.preprocessImageJson(stringifyOptions(options)))
fun recognize(options: PhironOcrRecognizeOptions): PhironOcrResult = parseResult(OcrNative.recognizeJson(stringifyOptions(options)))
fun recognizeScaleValue(options: PhironOcrRecognizeOptions): PhironOcrResult = parseResult(OcrNative.recognizeScaleValueJson(stringifyOptions(options)))

open class PhironOcrQuadPointJSONObject : UTSJSONObject() {
    open lateinit var x: Number
    open lateinit var y: Number
}

open class PhironOcrRoiJSONObject : UTSJSONObject() {
    open var left: Number? = null
    open var top: Number? = null
    open var width: Number? = null
    open var height: Number? = null
}

open class PhironOcrPreprocessOptionsJSONObject : UTSJSONObject() {
    open var enableGray: Boolean? = null
    open var enableDenoise: Boolean? = null
    open var enableSharpen: Boolean? = null
    open var enableContrast: Boolean? = null
    open var enableAdaptiveThreshold: Boolean? = null
    open var enableDeskew: Boolean? = null
    open var enablePerspectiveCorrection: Boolean? = null
    open var manualRotateDegrees: Number? = null
    open var cropRoi: PhironOcrRoiJSONObject? = null
    open var perspectivePoints: UTSArray<PhironOcrQuadPointJSONObject>? = null
    open var contrastAlpha: Number? = null
    open var contrastBeta: Number? = null
    open var adaptiveBlockSize: Number? = null
    open var adaptiveC: Number? = null
    open var denoiseKernelSize: Number? = null
    open var sharpenSigma: Number? = null
    open var outputPath: String? = null
    open var returnBase64: Boolean? = null
}

open class PhironOcrRecognizeOptionsJSONObject : UTSJSONObject() {
    open var imagePath: String? = null
    open var imageUri: String? = null
    open var base64: String? = null
    open var expectedRegex: String? = null
    open var allowedChars: String? = null
    open var maxResultCount: Number? = null
    open var preferDigits: Boolean? = null
    open var extractBestNumericCandidate: Boolean? = null
    open var includeRawBlocks: Boolean? = null
    open var preprocess: PhironOcrPreprocessOptionsJSONObject? = null
}
