package uts.sdk.modules.phironocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Base64
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.dcloud.uts.UTSAndroid
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

object OcrNative {
    private const val VERSION = "1.0.0"

    @JvmStatic
    fun getVersionJson(): String {
        return success(VERSION, "ok").toJSONString()
    }

    @JvmStatic
    fun checkEnvironmentJson(): String {
        return wrap {
            OpenCvImageProcessor.ensureOpenCvReady()
            val context = requireContext()
            val data = JSONObject()
            data["openCvReady"] = true
            data["packageName"] = context.packageName
            data["filesDir"] = context.filesDir.absolutePath
            data["cacheDir"] = context.cacheDir.absolutePath
            data["version"] = VERSION
            success(data, "environment ready")
        }
    }

    @JvmStatic
    fun preprocessImageJson(optionsJson: String?): String {
        return wrap {
            val options = parseOptions(optionsJson)
            val bitmap = decodeBitmap(options)
            val processed = OpenCvImageProcessor.preprocess(bitmap, options)
            val data = JSONObject()
            data["imageWidth"] = processed.bitmap.width
            data["imageHeight"] = processed.bitmap.height
            data["preprocess"] = processed.metadata
            options.getString("outputPath")?.takeIf { it.isNotBlank() }?.let { outputPath ->
                val file = writeBitmap(processed.bitmap, File(outputPath))
                data["outputPath"] = file.absolutePath
            }
            if (options.getBooleanValue("returnBase64")) {
                data["base64"] = bitmapToBase64(processed.bitmap)
            }
            success(data, "preprocess success")
        }
    }

    @JvmStatic
    fun recognizeJson(optionsJson: String?): String {
        return wrap {
            val options = parseOptions(optionsJson)
            val result = doRecognize(options, false)
            success(result, "recognize success")
        }
    }

    @JvmStatic
    fun recognizeScaleValueJson(optionsJson: String?): String {
        return wrap {
            val options = parseOptions(optionsJson)
            options["preferDigits"] = true
            options["extractBestNumericCandidate"] = true
            if (!options.containsKey("allowedChars")) {
                options["allowedChars"] = "0123456789.-"
            }
            if (!options.containsKey("expectedRegex")) {
                options["expectedRegex"] = "-?\\d+(?:\\.\\d+)?"
            }
            if (!options.containsKey("preprocess")) {
                options["preprocess"] = defaultScalePreprocess()
            }
            val result = doRecognize(options, true)
            success(result, "recognize scale success")
        }
    }

    private fun doRecognize(options: JSONObject, scaleMode: Boolean): JSONObject {
        val preprocessOptions = mergePreprocess(options.getJSONObject("preprocess"), scaleMode)
        val originalBitmap = decodeBitmap(options)
        val processed = OpenCvImageProcessor.preprocess(originalBitmap, preprocessOptions)
        val textResult = runMlKit(processed.bitmap)
        val blocks = JSONArray()
        val lineCandidates = ArrayList<Candidate>()

        textResult.textBlocks.forEach { block ->
            val blockJson = JSONObject()
            blockJson["text"] = block.text
            blockJson["box"] = rectToJson(block.boundingBox)
            val lines = JSONArray()
            block.lines.forEach { line ->
                val normalizedText = normalizeText(line.text, options)
                if (normalizedText.isNotBlank()) {
                    val candidate = buildCandidate(normalizedText, line.boundingBox, processed.bitmap.width, processed.bitmap.height, options)
                    lineCandidates.add(candidate)
                    lines.add(candidate.toJson())
                }
            }
            if (lines.isNotEmpty()) {
                blockJson["lines"] = lines
                blocks.add(blockJson)
            }
        }

        val sorted = lineCandidates.sortedByDescending { it.confidence }
        val limited = JSONArray()
        val maxCount = max(1, options.getIntValue("maxResultCount").let { if (it <= 0) 5 else it })
        sorted.take(maxCount).forEach { limited.add(it.toJson()) }

        val data = JSONObject()
        data["text"] = sorted.firstOrNull()?.text ?: ""
        data["confidence"] = sorted.firstOrNull()?.confidence ?: 0.0
        data["confidenceSource"] = "heuristic"
        data["lines"] = limited
        data["preprocess"] = processed.metadata
        data["rawText"] = textResult.text ?: ""
        if (options.getBooleanValue("includeRawBlocks")) {
            data["blocks"] = blocks
        }
        if (scaleMode || options.getBooleanValue("extractBestNumericCandidate")) {
            data["bestNumericCandidate"] = pickBestNumericCandidate(sorted, options)
        }
        preprocessOptions.getString("outputPath")?.takeIf { it.isNotBlank() }?.let { outputPath ->
            val file = writeBitmap(processed.bitmap, File(outputPath))
            data["outputPath"] = file.absolutePath
        }
        if (preprocessOptions.getBooleanValue("returnBase64")) {
            data["base64"] = bitmapToBase64(processed.bitmap)
        }
        return data
    }

    private fun runMlKit(bitmap: Bitmap): Text {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
        } finally {
            recognizer.close()
        }
    }

    private fun mergePreprocess(input: JSONObject?, scaleMode: Boolean): JSONObject {
        val output = defaultPreprocess()
        if (scaleMode) {
            val scalePreset = defaultScalePreprocess()
            scalePreset.forEach { key, value -> output[key] = value }
        }
        input?.forEach { key, value -> output[key] = value }
        return output
    }

    private fun defaultPreprocess(): JSONObject {
        return JSONObject().apply {
            this["enableGray"] = true
            this["enableDenoise"] = true
            this["enableSharpen"] = true
            this["enableContrast"] = true
            this["enableAdaptiveThreshold"] = false
            this["enableDeskew"] = false
            this["enablePerspectiveCorrection"] = false
            this["contrastAlpha"] = 1.45
            this["contrastBeta"] = 6
            this["adaptiveBlockSize"] = 31
            this["adaptiveC"] = 12
            this["denoiseKernelSize"] = 3
            this["sharpenSigma"] = 1.2
        }
    }

    private fun defaultScalePreprocess(): JSONObject {
        return JSONObject().apply {
            this["enableGray"] = true
            this["enableDenoise"] = true
            this["enableSharpen"] = true
            this["enableContrast"] = true
            this["enableAdaptiveThreshold"] = true
            this["enableDeskew"] = true
            this["contrastAlpha"] = 1.65
            this["contrastBeta"] = 10
            this["adaptiveBlockSize"] = 35
            this["adaptiveC"] = 10
            this["denoiseKernelSize"] = 3
            this["sharpenSigma"] = 1.0
        }
    }

    private fun buildCandidate(text: String, rect: Rect?, width: Int, height: Int, options: JSONObject): Candidate {
        val allowedChars = options.getString("allowedChars")
        val expectedRegex = options.getString("expectedRegex")
        val areaRatio = if (rect == null || width <= 0 || height <= 0) 0.0 else rect.width().toDouble() * rect.height() / (width.toDouble() * height.toDouble())
        val allowedRatio = computeAllowedRatio(text, allowedChars)
        val regexBoost = if (!expectedRegex.isNullOrBlank() && Pattern.compile(expectedRegex).matcher(text).find()) 0.2 else 0.0
        val digitBoost = if (options.getBooleanValue("preferDigits") && text.any { it.isDigit() }) 0.1 else 0.0
        val lengthScore = min(text.length / 8.0, 1.0) * 0.15
        val areaScore = min(areaRatio * 3.5, 0.2)
        val confidence = min(0.99, 0.25 + allowedRatio * 0.3 + regexBoost + digitBoost + lengthScore + areaScore)
        return Candidate(text, confidence, rectToJson(rect))
    }

    private fun pickBestNumericCandidate(candidates: List<Candidate>, options: JSONObject): JSONObject? {
        val regex = options.getString("expectedRegex")?.takeIf { it.isNotBlank() } ?: "-?\\d+(?:\\.\\d+)?"
        val pattern = Pattern.compile(regex)
        candidates.forEach { candidate ->
            val matcher = pattern.matcher(candidate.text)
            if (matcher.find()) {
                return JSONObject().apply {
                    this["text"] = matcher.group()
                    this["confidence"] = candidate.confidence
                    this["box"] = candidate.box
                    this["sourceText"] = candidate.text
                }
            }
        }
        return null
    }

    private fun normalizeText(text: String?, options: JSONObject): String {
        if (text.isNullOrBlank()) return ""
        val compact = text.replace("\\s+".toRegex(), "")
        return if (options.getBooleanValue("preferDigits")) {
            compact
                .replace('O', '0')
                .replace('o', '0')
                .replace('I', '1')
                .replace('l', '1')
                .replace('B', '8')
        } else {
            compact
        }
    }

    private fun computeAllowedRatio(text: String, allowedChars: String?): Double {
        if (allowedChars.isNullOrBlank()) return 1.0
        if (text.isEmpty()) return 0.0
        val allowed = allowedChars.toSet()
        val matched = text.count { allowed.contains(it) }
        return matched.toDouble() / text.length
    }

    private fun decodeBitmap(options: JSONObject): Bitmap {
        val path = options.getString("imagePath")
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("imagePath not found: $path")
            }
            return BitmapFactory.decodeFile(file.absolutePath)
                ?: throw IllegalArgumentException("failed to decode imagePath: $path")
        }
        val base64 = options.getString("base64")
        if (!base64.isNullOrBlank()) {
            val content = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(content, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("failed to decode base64 image")
        }
        throw IllegalArgumentException("imagePath or base64 is required")
    }

    private fun writeBitmap(bitmap: Bitmap, file: File): File {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun rectToJson(rect: Rect?): JSONObject? {
        if (rect == null) return null
        return JSONObject().apply {
            this["left"] = rect.left
            this["top"] = rect.top
            this["right"] = rect.right
            this["bottom"] = rect.bottom
            this["width"] = rect.width()
            this["height"] = rect.height()
        }
    }

    private fun parseOptions(optionsJson: String?): JSONObject {
        return if (optionsJson.isNullOrBlank()) JSONObject() else JSON.parseObject(optionsJson) ?: JSONObject()
    }

    private fun requireContext() = UTSAndroid.getAppContext() ?: throw IllegalStateException("app context unavailable")

    private fun wrap(block: () -> JSONObject): String {
        return try {
            block().toJSONString()
        } catch (throwable: Throwable) {
            error(-1, throwable.message ?: throwable.javaClass.simpleName).toJSONString()
        }
    }

    private fun success(data: Any?, message: String): JSONObject {
        return JSONObject().apply {
            this["success"] = true
            this["code"] = 0
            this["message"] = message
            this["data"] = data
        }
    }

    private fun error(code: Int, message: String): JSONObject {
        return JSONObject().apply {
            this["success"] = false
            this["code"] = code
            this["message"] = message
            this["data"] = null
        }
    }
}

data class Candidate(
    val text: String,
    val confidence: Double,
    val box: JSONObject?
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            this["text"] = text
            this["confidence"] = confidence
            this["box"] = box
        }
    }
}
