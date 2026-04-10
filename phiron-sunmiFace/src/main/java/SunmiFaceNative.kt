package uts.sdk.modules.phironsunmiFace

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.sunmi.authorizelibrary.SunmiAuthorizeSDK
import com.sunmi.authorizelibrary.bean.AuthorizeResult
import com.sunmi.authorizelibrary.constants.ErrorCode
import com.sunmi.facelib.SunmiFaceAge
import com.sunmi.facelib.SunmiFaceBoxSortMode
import com.sunmi.facelib.SunmiFaceCompareResult
import com.sunmi.facelib.SunmiFaceConfigParam
import com.sunmi.facelib.SunmiFaceDBIdInfo
import com.sunmi.facelib.SunmiFaceDBRecord
import com.sunmi.facelib.SunmiFaceFeature
import com.sunmi.facelib.SunmiFaceGender
import com.sunmi.facelib.SunmiFaceGenderType
import com.sunmi.facelib.SunmiFaceImage
import com.sunmi.facelib.SunmiFaceImageFeatures
import com.sunmi.facelib.SunmiFaceLib
import com.sunmi.facelib.SunmiFaceLibConstants
import com.sunmi.facelib.SunmiFaceLivenessMode
import com.sunmi.facelib.SunmiFaceMode
import com.sunmi.facelib.SunmiFacePose
import com.sunmi.facelib.SunmiFaceQualityMode
import com.sunmi.facelib.SunmiFaceRect
import com.sunmi.facelib.SunmiFaceSDK
import com.sunmi.facelib.SunmiFaceStatusCode
import io.dcloud.uts.UTSAndroid
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SunmiFaceNative {
    private const val METADATA_FILE_NAME = "face_records_meta.json"
    private const val FACE_PERMISSION_REQUEST_CODE = 20041
    private val featuresCache = ConcurrentHashMap<String, SunmiFaceImageFeatures>()
    private var handleCreated = false
    private var authorizeSdkInitialized = false

    @JvmStatic
    fun getVersionJson(): String {
        return success(SunmiFaceSDK.getVersion(), "ok").toJSONString()
    }

    @JvmStatic
    fun getDeviceInfoJson(): String {
        val context = getContext()
        val data = JSONObject()
        data["brand"] = Build.BRAND
        data["manufacturer"] = Build.MANUFACTURER
        data["model"] = Build.MODEL
        data["device"] = Build.DEVICE
        data["product"] = Build.PRODUCT
        data["hardware"] = Build.HARDWARE
        data["board"] = Build.BOARD
        data["fingerprint"] = Build.FINGERPRINT
        data["androidSdkInt"] = Build.VERSION.SDK_INT
        data["androidRelease"] = Build.VERSION.RELEASE
        data["supportedAbis"] = JSONArray().apply { addAll(Build.SUPPORTED_ABIS.toList()) }
        data["defaultLicensePath"] = "/storage/emulated/0/SunmiRemoteFiles/license_face.txt"
        data["defaultLicenseExists"] = File("/storage/emulated/0/SunmiRemoteFiles/license_face.txt").exists()
        data["defaultLicenseReadable"] = File("/storage/emulated/0/SunmiRemoteFiles/license_face.txt").canRead()
        if (context != null) {
            data["appFilesDir"] = context.filesDir.absolutePath
            data["appPackageName"] = context.packageName
        }
        return success(data, "device info success").toJSONString()
    }

    @JvmStatic
    fun initAuthorizeSDKJson(optionsJson: String?): String {
        return wrap {
            ensureAuthorizeRuntimeAvailable()
            val context = requireContext()
            val options = parseOptions(optionsJson)
            SunmiAuthorizeSDK.setDebuggable(options.getBooleanValue("debuggable"))
            SunmiAuthorizeSDK.init(context)
            authorizeSdkInitialized = true
            val data = JSONObject()
            data["debuggable"] = options.getBooleanValue("debuggable")
            data["sdkVersion"] = SunmiAuthorizeSDK.getSunmiAuthorizeSDKVersion()
            success(data, "init authorize sdk success")
        }
    }

    @JvmStatic
    fun getAuthorizeSDKVersionJson(): String {
        val data = JSONObject()
        data["code"] = ErrorCode.IS_SUCCESS
        data["success"] = true
        data["message"] = "ok"
        data["data"] = SunmiAuthorizeSDK.getSunmiAuthorizeSDKVersion()
        return data.toJSONString()
    }

    @JvmStatic
    fun syncGetAuthorizeCodeJson(optionsJson: String?): String {
        return wrap {
            ensureAuthorizeRuntimeAvailable()
            ensureAuthorizeSdk()
            val options = parseOptions(optionsJson)
            val result = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options))
            authorizeResult(result)
        }
    }

    @JvmStatic
    fun clearLocalTokenJson(): String {
        return wrap {
            ensureAuthorizeRuntimeAvailable()
            ensureAuthorizeSdk()
            SunmiAuthorizeSDK.clearLocalToken()
            success(null, "clear local token success")
        }
    }

    @JvmStatic
    fun createHandleJson(): String {
        return wrap {
            val code = SunmiFaceSDK.createHandle()
            handleCreated = code == SunmiFaceStatusCode.FACE_CODE_OK
            status(code, null, if (handleCreated) "create handle success" else null)
        }
    }

    @JvmStatic
    fun initJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val context = requireContext()
            val options = parseOptions(optionsJson)
            val configDir = ensureConfigDirectory(context, options)
            val configPath = resolveConfigPath(options.getString("configPath"), configDir)
            val code = SunmiFaceSDK.init(configPath)
            val data = JSONObject()
            data["configPath"] = configPath
            data["configDir"] = configDir.absolutePath
            status(code, data, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "init success" else null)
        }
    }

    @JvmStatic
    fun activateByLicensePathJson(optionsJson: String?): String {
        return wrap {
            val context = requireContext()
            val options = parseOptions(optionsJson)
            val licensePath = options.getString("licensePath")
            if (licensePath.isNullOrEmpty()) {
                error(SunmiFaceStatusCode.FACE_CODE_LICENSE_ERROR, "licensePath is required")
            } else {
                val licenseContent = readTextFile(File(normalizePath(licensePath)))
                val code = SunmiFaceSDK.verifyLicense(context, licenseContent)
                status(code, buildVerifyPayload(code, licensePath, licenseContent), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "activate by license path success" else null)
            }
        }
    }

    @JvmStatic
    fun activateByAppIdJson(optionsJson: String?): String {
        return wrap {
            ensureAuthorizeRuntimeAvailable()
            val context = requireContext()
            val options = parseOptions(optionsJson)
            ensureAuthorizeSdk()
            val authResult = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options))
            if (authResult == null || authResult.code != ErrorCode.IS_SUCCESS || authResult.token.isNullOrEmpty()) {
                val data = authorizePayload(authResult)
                errorWithData(authResult?.code ?: ErrorCode.REQUEST_EXCEPTION, authResult?.msg ?: "authorize failed", data)
            } else {
                val verifyCode = SunmiFaceSDK.verifyLicense(context, authResult.token)
                val data = JSONObject()
                data["authorize"] = authorizePayload(authResult)
                data["verify"] = buildVerifyPayload(verifyCode, null, authResult.token)
                status(verifyCode, data, if (verifyCode == SunmiFaceStatusCode.FACE_CODE_OK) "activate by appId success" else null)
            }
        }
    }

    @JvmStatic
    fun verifyLicenseJson(optionsJson: String?): String {
        return wrap {
            val context = requireContext()
            val options = parseOptions(optionsJson)
            val appId = options.getString("appId")
            val license = options.getString("license")
            val licensePath = options.getString("licensePath")
            if (!appId.isNullOrEmpty()) {
                ensureAuthorizeRuntimeAvailable()
                ensureAuthorizeSdk()
                val authResult = SunmiAuthorizeSDK.syncGetAuthorizeCode(buildAuthorizeParams(options))
                if (authResult == null || authResult.code != ErrorCode.IS_SUCCESS || authResult.token.isNullOrEmpty()) {
                    errorWithData(authResult?.code ?: ErrorCode.REQUEST_EXCEPTION, authResult?.msg ?: "authorize failed", authorizePayload(authResult))
                } else {
                    val code = SunmiFaceSDK.verifyLicense(context, authResult.token)
                    val data = JSONObject()
                    data["authorize"] = authorizePayload(authResult)
                    data["verify"] = buildVerifyPayload(code, null, authResult.token)
                    status(code, data, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "verify license success" else null)
                }
            } else {
                val content = when {
                    !license.isNullOrEmpty() -> license
                    !licensePath.isNullOrEmpty() -> readTextFile(File(normalizePath(licensePath)))
                    else -> throw IllegalArgumentException("appId or license/licensePath is required")
                }
                val code = SunmiFaceSDK.verifyLicense(context, content)
                status(code, buildVerifyPayload(code, licensePath, content), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "verify license success" else null)
            }
        }
    }

    @JvmStatic
    fun getErrorStringJson(optionsJson: String?): String {
        val code = parseOptions(optionsJson).getIntValue("code")
        val data = JSONObject()
        data["code"] = code
        data["errorString"] = SunmiFaceSDK.getErrorString(code)
        return success(data, "get error string success").toJSONString()
    }

    @JvmStatic
    fun checkPermissionsJson(): String {
        return wrap {
            val activity = requireActivity()
            val requiredPermissions = requiredRuntimePermissions()
            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }
            val data = JSONObject()
            val permissionStatus = JSONObject()
            requiredPermissions.forEach { permission ->
                permissionStatus[permission] =
                    ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
            data["requiredPermissions"] = JSONArray().apply { addAll(requiredPermissions) }
            data["missingPermissions"] = JSONArray().apply { addAll(missingPermissions) }
            data["allGranted"] = missingPermissions.isEmpty()
            data["requested"] = false
            val allFilesAccessRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val allFilesAccessGranted = !allFilesAccessRequired || Environment.isExternalStorageManager()
            permissionStatus["android.permission.MANAGE_EXTERNAL_STORAGE"] = allFilesAccessGranted
            data["allFilesAccessRequired"] = allFilesAccessRequired
            data["allFilesAccessGranted"] = allFilesAccessGranted
            data["specialPermissions"] = JSONArray().apply {
                if (allFilesAccessRequired) {
                    add("android.permission.MANAGE_EXTERNAL_STORAGE")
                }
            }
            data["specialMissingPermissions"] = JSONArray().apply {
                if (allFilesAccessRequired && !allFilesAccessGranted) {
                    add("android.permission.MANAGE_EXTERNAL_STORAGE")
                }
            }
            data["permissionStatus"] = permissionStatus
            if (missingPermissions.isEmpty() && allFilesAccessGranted) {
                success(data, "权限已全部授予")
            } else {
                if (missingPermissions.isNotEmpty()) {
                    ActivityCompat.requestPermissions(
                        activity,
                        missingPermissions.toTypedArray(),
                        FACE_PERMISSION_REQUEST_CODE
                    )
                }
                if (!allFilesAccessGranted) {
                    openAllFilesAccessSettings(activity)
                }
                data["requested"] = true
                success(data, if (!allFilesAccessGranted) "已拉起系统权限授权弹窗和文件管理权限设置页" else "已拉起系统权限授权弹窗")
            }
        }
    }

    @JvmStatic
    fun setConfigJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val param = buildConfigParam(parseOptions(optionsJson))
            val code = SunmiFaceSDK.setConfig(param)
            status(code, configToJson(param), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "set config success" else null)
        }
    }

    @JvmStatic
    fun getConfigJson(): String {
        return wrap {
            ensureHandle()
            val param = SunmiFaceConfigParam()
            val code = SunmiFaceSDK.getConfig(param)
            status(code, configToJson(param), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "get config success" else null)
        }
    }

    @JvmStatic
    fun initDBJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val dbPath = resolveDbFilePath(parseOptions(optionsJson))
            ensureParentDirectory(File(dbPath))
            val code = SunmiFaceSDK.initDB(dbPath)
            val data = JSONObject()
            data["dbPath"] = dbPath
            status(code, data, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "init db success" else null)
        }
    }

    @JvmStatic
    fun getImageFeaturesJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val options = parseOptions(optionsJson)
            val result = extractFeatures(options)
            try {
                status(result.code, result.toJson(), if (result.code == SunmiFaceStatusCode.FACE_CODE_OK) "get image features success" else null)
            } finally {
                if (result.owned && result.imageFeatures != null) {
                    try {
                        SunmiFaceSDK.releaseImageFeatures(result.imageFeatures)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    @JvmStatic
    fun releaseImageFeaturesJson(optionsJson: String?): String {
        return wrap {
            val token = parseOptions(optionsJson).getString("token")
            val features = if (token.isNullOrEmpty()) null else featuresCache.remove(token)
            if (features == null) {
                success(null, "image features already released")
            } else {
                val code = SunmiFaceSDK.releaseImageFeatures(features)
                status(code, null, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "release image features success" else null)
            }
        }
    }

    @JvmStatic
    fun addDBRecordJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val options = parseOptions(optionsJson)
            val id = options.getString("id")
            if (id.isNullOrEmpty()) {
                error(SunmiFaceStatusCode.FACE_CODE_IMAGE_ID_ERROR, "id is required")
            } else {
                val carrier = resolveFeatureCarrier(options)
                try {
                    if (carrier.feature == null || carrier.feature.feature == null) {
                        error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "feature is required")
                    } else {
                        val record = SunmiFaceSDK.faceFeature2FaceDBRecord(carrier.feature)
                        record.id = id
                        record.name = options.getString("name")
                        record.imgId = options.getString("imgId")
                        val code = SunmiFaceSDK.addDBRecord(record)
                        if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                            saveMetadataRecord(options, record, carrier.feature)
                        }
                        status(code, dbRecordToJson(record), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "add db record success" else null)
                    }
                } finally {
                    releaseCarrier(carrier)
                }
            }
        }
    }

    @JvmStatic
    fun searchDBJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val options = parseOptions(optionsJson)
            val carrier = resolveFeatureCarrier(options)
            try {
                if (carrier.feature == null || carrier.feature.feature == null) {
                    error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "feature is required")
                } else {
                    val record = SunmiFaceSDK.faceFeature2FaceDBRecord(carrier.feature)
                    val info = SunmiFaceDBIdInfo()
                    val code = SunmiFaceSDK.searchDB(record, info)
                    val data = dbIdInfoToJson(info)
                    if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                        val metadata = findFirstMetadataRecord(options, info.id)
                        if (metadata != null) {
                            data["metadata"] = metadata
                        }
                    }
                    status(code, data, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "search db success" else null)
                }
            } finally {
                releaseCarrier(carrier)
            }
        }
    }

    @JvmStatic
    fun compare1v1Json(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val options = parseOptions(optionsJson)
            val first = resolveFeatureCarrier(options.getJSONObject("first") ?: JSONObject())
            val second = resolveFeatureCarrier(options.getJSONObject("second") ?: JSONObject())
            try {
                if (first.feature == null || second.feature == null) {
                    error(SunmiFaceStatusCode.FACE_CODE_EMPTY_IMAGE, "failed to resolve compare features")
                } else {
                    val result = SunmiFaceCompareResult()
                    val code = SunmiFaceSDK.compare1v1(first.feature, second.feature, result)
                    status(code, compareResultToJson(result), if (code == SunmiFaceStatusCode.FACE_CODE_OK) "compare success" else null)
                }
            } finally {
                releaseCarrier(first)
                releaseCarrier(second)
            }
        }
    }

    @JvmStatic
    fun deleteDBRecordJson(optionsJson: String?): String {
        return wrap {
            ensureHandle()
            val options = parseOptions(optionsJson)
            val id = options.getString("id")
            val imgId = options.getString("imgId")
            if (id.isNullOrEmpty() && imgId.isNullOrEmpty()) {
                error(SunmiFaceStatusCode.FACE_CODE_IMAGE_ID_ERROR, "imgId or id is required")
            } else {
                val metadataFile = resolveMetadataFile(options)
                val deletedImgIds = JSONArray()
                var code: Int
                if (!imgId.isNullOrEmpty()) {
                    code = SunmiFaceSDK.deleteDBRecord(imgId)
                    if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
                        deletedImgIds.add(imgId)
                    }
                } else {
                    val imgIds = findImgIdsById(metadataFile, id!!)
                    if (imgIds.isEmpty()) {
                        code = SunmiFaceStatusCode.FACE_CODE_IMAGE_ID_ERROR
                    } else {
                        code = SunmiFaceStatusCode.FACE_CODE_OK
                        for (i in 0 until imgIds.size) {
                            val currentImgId = imgIds.getString(i)
                            val deleteCode = SunmiFaceSDK.deleteDBRecord(currentImgId)
                            if (deleteCode == SunmiFaceStatusCode.FACE_CODE_OK) {
                                deletedImgIds.add(currentImgId)
                            } else {
                                code = deleteCode
                            }
                        }
                    }
                }
                if (!deletedImgIds.isEmpty()) {
                    removeMetadataRecords(metadataFile, deletedImgIds)
                }
                val data = JSONObject()
                data["deletedImgIds"] = deletedImgIds
                status(code, data, if (code == SunmiFaceStatusCode.FACE_CODE_OK) "delete db record success" else null)
            }
        }
    }

    @JvmStatic
    fun getAllDBRecordsJson(optionsJson: String?): String {
        return wrap {
            val records = readMetadataArray(resolveMetadataFile(parseOptions(optionsJson)))
            success(records, "get all db records success")
        }
    }

    @JvmStatic
    fun clearFaceDatabaseJson(optionsJson: String?): String {
        return wrap {
            val options = parseOptions(optionsJson)
            val dbFile = File(resolveDbFilePath(options))
            val metadataFile = resolveMetadataFile(options)
            var deletedDb = false
            var deletedMetadata = false
            if (dbFile.exists()) {
                deletedDb = dbFile.delete()
            }
            if (metadataFile.exists()) {
                deletedMetadata = metadataFile.delete()
            }
            val data = JSONObject()
            data["dbPath"] = dbFile.absolutePath
            data["metadataPath"] = metadataFile.absolutePath
            data["deletedDb"] = deletedDb
            data["deletedMetadata"] = deletedMetadata
            success(data, "clear face database success")
        }
    }

    @JvmStatic
    fun releaseHandleJson(): String {
        releaseAllCachedFeatures()
        val hadHandle = handleCreated
        handleCreated = false
        try {
            SunmiFaceSDK.releaseHandle()
        } catch (_: Exception) {
        }
        return success(null, if (hadHandle) "release handle success" else "handle already released").toJSONString()
    }

    private fun extractFeatures(options: JSONObject): ExtractionResult {
        ensureHandle()
        if (options.containsKey("threadNum")
            || options.containsKey("distanceThreshold")
            || options.containsKey("faceScoreThreshold")
            || options.containsKey("minFaceSize")
            || options.containsKey("depthXOffset")
            || options.containsKey("depthYOffset")
            || options.containsKey("boxSortMode")
        ) {
            val code = SunmiFaceSDK.setConfig(buildConfigParam(options))
            if (code != SunmiFaceStatusCode.FACE_CODE_OK) {
                throw IllegalStateException("setConfig failed: ${SunmiFaceSDK.getErrorString(code)}")
            }
        }

        val imageResult = buildImage(options)
        val imageFeatures = SunmiFaceImageFeatures()
        val result = ExtractionResult()
        try {
            val code = SunmiFaceSDK.getImageFeatures(imageResult.image, imageFeatures)
            result.code = code
            result.imageFeatures = imageFeatures
            result.data = minimalFeatureContainerToJson(imageFeatures, imageResult.width, imageResult.height)
            if (code != SunmiFaceStatusCode.FACE_CODE_OK) {
                return result
            }
            val primaryFeature = getPrimaryFeature(imageFeatures)
            result.feature = primaryFeature
            result.data = featureContainerToJson(imageFeatures, options, imageResult.width, imageResult.height)
            if (options.getBooleanValue("keepAlive")) {
                val token = UUID.randomUUID().toString()
                featuresCache[token] = imageFeatures
                result.token = token
            } else {
                result.owned = true
            }
            return result
        } finally {
            try {
                imageResult.image.delete()
            } catch (_: Exception) {
            }
            if (!result.owned && result.token.isNullOrEmpty() && result.feature != null) {
                try {
                    result.feature!!.delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun resolveFeatureCarrier(options: JSONObject): FeatureCarrier {
        val token = options.getString("token")
        if (!token.isNullOrEmpty() && featuresCache.containsKey(token)) {
            val retainToken = options.getBooleanValue("retainToken")
            val imageFeatures = if (retainToken) featuresCache[token] else featuresCache.remove(token)
            return FeatureCarrier(getPrimaryFeature(imageFeatures), imageFeatures, !retainToken)
        }
        val featureJson = options.getJSONArray("feature")
        if (featureJson != null && !featureJson.isEmpty()) {
            val feature = SunmiFaceFeature()
            feature.feature = toFloatArray(featureJson)
            return FeatureCarrier(feature, null, false)
        }
        val result = extractFeatures(options)
        if (result.code != SunmiFaceStatusCode.FACE_CODE_OK) {
            throw IllegalStateException("getImageFeatures failed: ${SunmiFaceSDK.getErrorString(result.code)}")
        }
        return FeatureCarrier(result.feature, result.imageFeatures, result.owned)
    }

    private fun releaseCarrier(carrier: FeatureCarrier?) {
        if (carrier?.feature != null) {
            try {
                carrier.feature.delete()
            } catch (_: Exception) {
            }
        }
        if (carrier?.owned == true && carrier.imageFeatures != null) {
            try {
                SunmiFaceSDK.releaseImageFeatures(carrier.imageFeatures)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildImage(options: JSONObject): ImageBuildResult {
        val imageBytes = resolveImageBytes(options) ?: throw IllegalArgumentException("imagePath or base64 is required")
        val decodeMaxSize = maxOf(64, options.getIntValue("decodeMaxSize").takeIf { it > 0 } ?: 1280)
        val bitmap = decodeBitmapForFace(imageBytes, decodeMaxSize) ?: throw IllegalArgumentException("failed to decode image")
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val rgb = ByteArray(width * height * 3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            rgb[i * 3] = (pixel and 0xFF).toByte()
            rgb[i * 3 + 1] = ((pixel shr 8) and 0xFF).toByte()
            rgb[i * 3 + 2] = ((pixel shr 16) and 0xFF).toByte()
        }
        bitmap.recycle()
        val image = SunmiFaceImage(rgb, height, width, options.getIntValue("maxFaceCount").takeIf { it > 0 } ?: 1)
        image.setPredictMode(options.getIntValue("predictMode").takeIf { it > 0 } ?: SunmiFaceMode.PredictMode_Feature)
        image.setLivenessMode(options.getIntValue("livenessMode"))
        image.setQualityMode(options.getIntValue("qualityMode"))
        return ImageBuildResult(image, width, height)
    }

    private fun decodeBitmapForFace(imageBytes: ByteArray, decodeMaxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inSampleSize = calculateInSampleSize(bounds, decodeMaxSize, decodeMaxSize)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
            inSampleSize *= 2
        }
        return maxOf(1, inSampleSize)
    }

    private fun resolveImageBytes(options: JSONObject): ByteArray? {
        val imagePath = options.getString("imagePath")
        if (!imagePath.isNullOrEmpty()) {
            return readFileBytes(File(normalizePath(imagePath)))
        }
        var base64 = options.getString("base64")
        if (base64.isNullOrEmpty()) {
            return null
        }
        val comma = base64.indexOf(',')
        if (comma >= 0) {
            base64 = base64.substring(comma + 1)
        }
        return Base64.decode(base64.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
    }

    private fun buildConfigParam(options: JSONObject): SunmiFaceConfigParam {
        val param = SunmiFaceConfigParam()
        if (options.containsKey("threadNum")) param.threadNum = options.getIntValue("threadNum")
        if (options.containsKey("distanceThreshold")) param.distanceThreshold = options.getFloatValue("distanceThreshold")
        if (options.containsKey("faceScoreThreshold")) param.faceScoreThreshold = options.getFloatValue("faceScoreThreshold")
        if (options.containsKey("minFaceSize")) param.minFaceSize = options.getIntValue("minFaceSize")
        if (options.containsKey("depthXOffset")) param.depthXOffset = options.getIntValue("depthXOffset")
        if (options.containsKey("depthYOffset")) param.depthYOffset = options.getIntValue("depthYOffset")
        if (options.containsKey("boxSortMode")) {
            param.boxSortMode = options.getIntValue("boxSortMode")
        } else {
            param.boxSortMode = SunmiFaceBoxSortMode.BoxSortMode_Score
        }
        return param
    }

    private fun configToJson(param: SunmiFaceConfigParam): JSONObject {
        val data = JSONObject()
        data["threadNum"] = param.threadNum
        data["distanceThreshold"] = param.distanceThreshold
        data["faceScoreThreshold"] = param.faceScoreThreshold
        data["minFaceSize"] = param.minFaceSize
        data["depthXOffset"] = param.depthXOffset
        data["depthYOffset"] = param.depthYOffset
        data["boxSortMode"] = param.boxSortMode
        return data
    }

    private fun featureContainerToJson(imageFeatures: SunmiFaceImageFeatures, options: JSONObject, imageWidth: Int, imageHeight: Int): JSONObject {
        val rectRotation = options.getIntValue("rectRotation")
        val rectMirrorX = options.getBooleanValue("rectMirrorX")
        val normRot = ((rectRotation % 360) + 360) % 360
        val outputWidth = if (normRot == 90 || normRot == 270) imageHeight else imageWidth
        val outputHeight = if (normRot == 90 || normRot == 270) imageWidth else imageHeight
        val data = JSONObject()
        data["imageWidth"] = imageWidth
        data["imageHeight"] = imageHeight
        data["outputImageWidth"] = outputWidth
        data["outputImageHeight"] = outputHeight
        data["featuresCount"] = imageFeatures.featuresCount
        data["feature"] = featureToJson(getPrimaryFeature(imageFeatures), imageWidth, imageHeight, normRot, rectMirrorX)
        return data
    }

    private fun minimalFeatureContainerToJson(imageFeatures: SunmiFaceImageFeatures, imageWidth: Int, imageHeight: Int): JSONObject {
        val data = JSONObject()
        data["imageWidth"] = imageWidth
        data["imageHeight"] = imageHeight
        data["outputImageWidth"] = imageWidth
        data["outputImageHeight"] = imageHeight
        data["featuresCount"] = imageFeatures.featuresCount
        data["safeMode"] = true
        return data
    }

    private fun getPrimaryFeature(imageFeatures: SunmiFaceImageFeatures?): SunmiFaceFeature? {
        if (imageFeatures == null || imageFeatures.featuresCount <= 0 || imageFeatures.features == null) {
            return null
        }
        return SunmiFaceLib.SunmiFaceFeatureArrayGetItem(imageFeatures.features, 0)
    }

    private fun featureToJson(feature: SunmiFaceFeature?, imageWidth: Int, imageHeight: Int, rectRotationDeg: Int, rectMirrorX: Boolean): JSONObject? {
        if (feature == null) return null
        val data = JSONObject()
        data["faceRect"] = rectToJson(feature.faceRect, imageWidth, imageHeight, rectRotationDeg, rectMirrorX)
        data["rgbLivenessScore"] = feature.rgbLivenessScore
        data["nirLivenessScore"] = feature.nirLivenessScore
        data["depthLivenessScore"] = feature.depthLivenessScore
        data["feature"] = floatArrayToJson(feature.feature)
        data["pose"] = poseToJson(feature.pose)
        data["age"] = ageToJson(feature.age)
        data["gender"] = genderToJson(feature.gender)
        data["varLaplacian"] = feature.varLaplacian
        data["luminance"] = feature.luminance
        data["occlusionScore"] = feature.occlusionScore
        return data
    }

    private fun rectToJson(rect: SunmiFaceRect?, imageWidth: Int, imageHeight: Int, rectRotationDeg: Int, rectMirrorX: Boolean): JSONObject? {
        if (rect == null) return null
        val left = minOf(rect.x1, rect.x2)
        val right = maxOf(rect.x1, rect.x2)
        val top = minOf(rect.y1, rect.y2)
        val bottom = maxOf(rect.y1, rect.y2)
        val p1 = transformPoint(left, top, imageWidth, imageHeight, rectRotationDeg, rectMirrorX)
        val p2 = transformPoint(right, top, imageWidth, imageHeight, rectRotationDeg, rectMirrorX)
        val p3 = transformPoint(left, bottom, imageWidth, imageHeight, rectRotationDeg, rectMirrorX)
        val p4 = transformPoint(right, bottom, imageWidth, imageHeight, rectRotationDeg, rectMirrorX)
        val data = JSONObject()
        data["x1"] = minOf(minOf(p1.first, p2.first), minOf(p3.first, p4.first))
        data["y1"] = minOf(minOf(p1.second, p2.second), minOf(p3.second, p4.second))
        data["x2"] = maxOf(maxOf(p1.first, p2.first), maxOf(p3.first, p4.first))
        data["y2"] = maxOf(maxOf(p1.second, p2.second), maxOf(p3.second, p4.second))
        data["score"] = rect.score
        return data
    }

    private fun transformPoint(x: Float, y: Float, imageWidth: Int, imageHeight: Int, rectRotationDeg: Int, rectMirrorX: Boolean): Pair<Float, Float> {
        var px = x
        val py = y
        if (rectMirrorX) {
            px = imageWidth - px
        }
        return when (rectRotationDeg) {
            90 -> Pair(imageHeight - py, px)
            180 -> Pair(imageWidth - px, imageHeight - py)
            270 -> Pair(py, imageWidth - px)
            else -> Pair(px, py)
        }
    }

    private fun poseToJson(pose: SunmiFacePose?): JSONObject? {
        if (pose == null) return null
        val data = JSONObject()
        data["pitch"] = pose.pitch
        data["yaw"] = pose.yaw
        data["roll"] = pose.roll
        return data
    }

    private fun ageToJson(age: SunmiFaceAge?): JSONObject? {
        if (age == null) return null
        val data = JSONObject()
        data["classification"] = age.classification
        data["score"] = age.score
        return data
    }

    private fun genderToJson(gender: SunmiFaceGender?): JSONObject? {
        if (gender == null) return null
        val data = JSONObject()
        data["classification"] = gender.classification
        data["score"] = gender.score
        data["type"] = if (gender.classification == SunmiFaceGenderType.FACE_ATTR_MALE) "male" else "female"
        return data
    }

    private fun compareResultToJson(result: SunmiFaceCompareResult): JSONObject {
        val data = JSONObject()
        data["isMatched"] = result.isMatched
        data["distance"] = result.distance
        return data
    }

    private fun dbRecordToJson(record: SunmiFaceDBRecord): JSONObject {
        val data = JSONObject()
        data["id"] = record.id
        data["name"] = record.name
        data["imgId"] = record.imgId
        data["feature"] = floatArrayToJson(record.feature)
        return data
    }

    private fun dbIdInfoToJson(info: SunmiFaceDBIdInfo): JSONObject {
        val data = JSONObject()
        data["id"] = info.id
        data["name"] = info.name
        data["isMatched"] = info.isMatched
        data["distance"] = info.distance
        return data
    }

    private fun buildAuthorizeParams(options: JSONObject): HashMap<String, Any> {
        val appId = options.getString("appId")
        if (appId.isNullOrEmpty()) {
            throw IllegalArgumentException("appId is required")
        }
        return hashMapOf(
            SunmiAuthorizeSDK.APP_ID to appId,
            SunmiAuthorizeSDK.CATEGORY_TYPE_KEY to SunmiAuthorizeSDK.CATEGORY_TYPE_FACE,
            SunmiAuthorizeSDK.IS_FORCE_REFRESH to options.getBooleanValue("forceRefresh")
        )
    }

    private fun authorizeResult(result: AuthorizeResult?): JSONObject {
        val wrapper = JSONObject()
        wrapper["code"] = result?.code ?: ErrorCode.REQUEST_EXCEPTION
        wrapper["success"] = result != null && result.code == ErrorCode.IS_SUCCESS && !result.token.isNullOrEmpty()
        wrapper["message"] = result?.msg ?: "authorize result is null"
        wrapper["data"] = authorizePayload(result)
        return wrapper
    }

    private fun authorizePayload(result: AuthorizeResult?): JSONObject {
        val data = JSONObject()
        data["code"] = result?.code ?: ErrorCode.REQUEST_EXCEPTION
        data["msg"] = result?.msg ?: "authorize result is null"
        data["token"] = result?.token
        return data
    }

    private fun buildVerifyPayload(code: Int, licensePath: String?, license: String?): JSONObject {
        val data = JSONObject()
        data["licensePath"] = licensePath
        data["license"] = license
        data["errorString"] = SunmiFaceSDK.getErrorString(code)
        return data
    }

    private fun ensureAuthorizeSdk() {
        if (!authorizeSdkInitialized) {
            SunmiAuthorizeSDK.init(requireContext())
            authorizeSdkInitialized = true
        }
    }

    private fun ensureAuthorizeRuntimeAvailable() {
        val missing = mutableListOf<String>()
        val requiredClasses = listOf(
            "com.sunmilib.service.HttpConfig\$Builder",
            "com.sunmilib.http.Request",
            "com.sunmilib.http.BaseResponse",
            "io.reactivex.Single"
        )
        for (name in requiredClasses) {
            try {
                Class.forName(name)
            } catch (_: Throwable) {
                missing.add(name)
            }
        }
        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "SunmiAuthorize-SDK runtime dependencies are missing: ${missing.joinToString(", ")}"
            )
        }
    }

    private fun ensureHandle() {
        if (!handleCreated) {
            val code = SunmiFaceSDK.createHandle()
            handleCreated = code == SunmiFaceStatusCode.FACE_CODE_OK
            if (!handleCreated) {
                throw IllegalStateException("createHandle failed: ${SunmiFaceSDK.getErrorString(code)}")
            }
        }
    }

    private fun requiredRuntimePermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return permissions.distinct()
    }

    private fun openAllFilesAccessSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }

    private fun ensureConfigDirectory(context: Context, options: JSONObject): File {
        val configPath = options.getString("configPath")
        val targetDir = if (!configPath.isNullOrEmpty()) {
            val file = File(normalizePath(configPath))
            if (file.isDirectory) file else file.parentFile
        } else {
            File(context.filesDir, "sunmi-face/config")
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val assets = arrayOf(
            "attribute.model",
            "config.json",
            "depth_detector.yml",
            "detect.model",
            "detect_new.model",
            "face.model",
            "face_occlusion.model",
            "head_pose.model",
            "nir_liveness.model",
            "rgb_liveness.model"
        )
        for (name in assets) {
            copyAssetIfNeeded(context, "config/$name", File(targetDir, name))
        }
        rewriteConfigJson(targetDir)
        return targetDir
    }

    private fun rewriteConfigJson(configDir: File) {
        val configFile = File(configDir, "config.json")
        val jsonObject = JSON.parseObject(readTextFile(configFile))
        absolutizeConfigValue(jsonObject, configDir, "face_model_path")
        absolutizeConfigValue(jsonObject, configDir, "detect_model_path")
        absolutizeConfigValue(jsonObject, configDir, "rgb_liveness_model_path")
        absolutizeConfigValue(jsonObject, configDir, "nir_liveness_model_path")
        absolutizeConfigValue(jsonObject, configDir, "attr_model_path")
        absolutizeConfigValue(jsonObject, configDir, "occlusion_model_path")
        absolutizeConfigValue(jsonObject, configDir, "headpose_model_path")
        absolutizeConfigValue(jsonObject, configDir, "depth_detector")
        absolutizeConfigValue(jsonObject, configDir, "face_db_file")
        writeTextFile(configFile, jsonObject.toJSONString())
    }

    private fun absolutizeConfigValue(jsonObject: JSONObject, baseDir: File, key: String) {
        val value = jsonObject.getString(key) ?: return
        val file = File(normalizePath(value))
        if (!file.isAbsolute) {
            jsonObject[key] = File(baseDir, value).absolutePath
        }
    }

    private fun resolveConfigPath(configPath: String?, configDir: File): String {
        if (!configPath.isNullOrEmpty()) {
            val file = File(normalizePath(configPath))
            return if (file.isDirectory) File(file, "config.json").absolutePath else file.absolutePath
        }
        return File(configDir, "config.json").absolutePath
    }

    private fun resolveDbFilePath(options: JSONObject): String {
        val dbPath = options.getString("dbPath")
        val baseDir = File(requireContext().filesDir, "sunmi-face/db")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        if (dbPath.isNullOrEmpty()) {
            return File(baseDir, "sunmi_face.db").absolutePath
        }
        val file = File(normalizePath(dbPath))
        return if (file.isDirectory || !file.name.contains(".")) File(file, "sunmi_face.db").absolutePath else file.absolutePath
    }

    private fun resolveMetadataFile(options: JSONObject): File {
        val dbFile = File(resolveDbFilePath(options))
        val parent = dbFile.parentFile ?: throw IOException("invalid db path")
        return File(parent, METADATA_FILE_NAME)
    }

    private fun saveMetadataRecord(options: JSONObject, record: SunmiFaceDBRecord, feature: SunmiFaceFeature) {
        val metadataFile = resolveMetadataFile(options)
        val records = readMetadataArray(metadataFile)
        val imgId = record.imgId ?: options.getString("imgId")
        val item = JSONObject()
        item["imgId"] = imgId
        item["id"] = record.id
        item["name"] = record.name
        item["phone"] = options.getString("phone")
        item["photoPath"] = firstNonEmpty(options.getString("photoPath"), options.getString("imagePath"))
        item["feature"] = floatArrayToJson(feature.feature)
        item["createdAt"] = System.currentTimeMillis()
        var replaced = false
        for (i in 0 until records.size) {
            val current = records.getJSONObject(i)
            if (current != null && current.getString("imgId") == imgId) {
                records[i] = item
                replaced = true
                break
            }
        }
        if (!replaced) {
            records.add(item)
        }
        writeMetadataArray(metadataFile, records)
    }

    private fun findFirstMetadataRecord(options: JSONObject, id: String?): JSONObject? {
        if (id.isNullOrEmpty()) return null
        val records = readMetadataArray(resolveMetadataFile(options))
        for (i in 0 until records.size) {
            val item = records.getJSONObject(i)
            if (item != null && item.getString("id") == id) {
                return item
            }
        }
        return null
    }

    private fun findImgIdsById(metadataFile: File, id: String): JSONArray {
        val imgIds = JSONArray()
        val records = readMetadataArray(metadataFile)
        for (i in 0 until records.size) {
            val item = records.getJSONObject(i)
            if (item != null && item.getString("id") == id) {
                item.getString("imgId")?.let { if (it.isNotEmpty()) imgIds.add(it) }
            }
        }
        return imgIds
    }

    private fun removeMetadataRecords(metadataFile: File, deletedImgIds: JSONArray) {
        val records = readMetadataArray(metadataFile)
        val next = JSONArray()
        for (i in 0 until records.size) {
            val item = records.getJSONObject(i)
            val imgId = item?.getString("imgId")
            if (!containsString(deletedImgIds, imgId)) {
                next.add(item)
            }
        }
        writeMetadataArray(metadataFile, next)
    }

    private fun readMetadataArray(metadataFile: File): JSONArray {
        if (!metadataFile.exists() || metadataFile.length() <= 0) return JSONArray()
        return JSON.parseArray(readTextFile(metadataFile)) ?: JSONArray()
    }

    private fun writeMetadataArray(metadataFile: File, records: JSONArray) {
        ensureParentDirectory(metadataFile)
        writeTextFile(metadataFile, records.toJSONString())
    }

    private fun containsString(array: JSONArray, value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        for (i in 0 until array.size) {
            if (value == array.getString(i)) return true
        }
        return false
    }

    private fun releaseAllCachedFeatures() {
        featuresCache.values.forEach {
            try {
                SunmiFaceSDK.releaseImageFeatures(it)
            } catch (_: Exception) {
            }
        }
        featuresCache.clear()
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String, targetFile: File) {
        if (targetFile.exists() && targetFile.length() > 0) return
        ensureParentDirectory(targetFile)
        context.assets.open(assetName).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val len = input.read(buffer)
                    if (len == -1) break
                    output.write(buffer, 0, len)
                }
                output.flush()
            }
        }
    }

    private fun readFileBytes(file: File): ByteArray {
        FileInputStream(file).use { input ->
            return input.readBytes()
        }
    }

    private fun readTextFile(file: File): String {
        return String(readFileBytes(file), StandardCharsets.UTF_8)
    }

    private fun writeTextFile(file: File, content: String) {
        ensureParentDirectory(file)
        FileOutputStream(file, false).use { output ->
            output.write(content.toByteArray(StandardCharsets.UTF_8))
            output.flush()
        }
    }

    private fun ensureParentDirectory(file: File) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
    }

    private fun normalizePath(path: String): String {
        return if (path.startsWith("file://")) path.removePrefix("file://") else path
    }

    private fun parseOptions(optionsJson: String?): JSONObject {
        return if (optionsJson.isNullOrEmpty()) JSONObject() else (JSON.parseObject(optionsJson) ?: JSONObject())
    }

    private fun success(data: Any?, message: String): JSONObject {
        val result = JSONObject()
        result["code"] = SunmiFaceStatusCode.FACE_CODE_OK
        result["success"] = true
        result["message"] = message
        result["data"] = data
        return result
    }

    private fun error(code: Int, message: String): JSONObject {
        val result = JSONObject()
        result["code"] = code
        result["success"] = false
        result["message"] = message
        result["errorString"] = SunmiFaceSDK.getErrorString(code)
        result["data"] = null
        return result
    }

    private fun errorWithData(code: Int, message: String, data: Any?): JSONObject {
        val result = error(code, message)
        result["data"] = data
        return result
    }

    private fun status(code: Int, data: Any?, successMessage: String?): JSONObject {
        return if (code == SunmiFaceStatusCode.FACE_CODE_OK) {
            success(data, successMessage ?: "ok")
        } else {
            val result = JSONObject()
            result["code"] = code
            result["success"] = false
            result["message"] = SunmiFaceSDK.getErrorString(code)
            result["errorString"] = SunmiFaceSDK.getErrorString(code)
            result["data"] = data
            result
        }
    }

    private fun wrap(block: () -> JSONObject): String {
        return try {
            block().toJSONString()
        } catch (e: Throwable) {
            val result = JSONObject()
            result["code"] = SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR
            result["success"] = false
            result["message"] = e.message ?: e.toString()
            result["errorString"] = e.toString()
            result["data"] = null
            result.toJSONString()
        }
    }

    private fun requireContext(): Context {
        return UTSAndroid.getAppContext() ?: throw IllegalStateException("context is null")
    }

    private fun requireActivity(): Activity {
        return UTSAndroid.getUniActivity() ?: throw IllegalStateException("activity is null")
    }

    private fun getContext(): Context? = UTSAndroid.getAppContext()

    private fun toFloatArray(array: JSONArray): FloatArray {
        val result = FloatArray(array.size)
        for (i in 0 until array.size) {
            result[i] = array.getFloatValue(i)
        }
        return result
    }

    private fun floatArrayToJson(values: FloatArray?): JSONArray? {
        if (values == null) return null
        val array = JSONArray()
        values.forEach { array.add(it) }
        return array
    }

    private fun firstNonEmpty(first: String?, second: String?): String? {
        return if (first.isNullOrEmpty()) second else first
    }

    private data class FeatureCarrier(
        val feature: SunmiFaceFeature?,
        val imageFeatures: SunmiFaceImageFeatures?,
        val owned: Boolean
    )

    private class ExtractionResult {
        var code: Int = SunmiFaceStatusCode.FACE_CODE_OTHER_ERROR
        var owned: Boolean = false
        var token: String? = null
        var feature: SunmiFaceFeature? = null
        var imageFeatures: SunmiFaceImageFeatures? = null
        var data: JSONObject = JSONObject()

        fun toJson(): JSONObject {
            val result = JSONObject()
            result.putAll(data)
            if (!token.isNullOrEmpty()) {
                result["token"] = token
            }
            return result
        }
    }

    private data class ImageBuildResult(
        val image: SunmiFaceImage,
        val width: Int,
        val height: Int
    )
}
