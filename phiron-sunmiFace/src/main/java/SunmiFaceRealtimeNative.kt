package uts.sdk.modules.phironsunmiFace

import android.app.Activity
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.dcloud.uts.UTSAndroid

object SunmiFaceRealtimeNative {
    private var activeDetectOverlay: SunmiFaceDetectOverlay? = null

    @JvmStatic
    fun startFaceRecognizeJson(optionsJson: String?, callback: ((String) -> Unit)?): String {
        return wrap {
            val activity = requireActivity()
            stopActiveDetectOverlay("replaced", "已替换上一层人脸识别", callback)
            val options = parseOptions(optionsJson)
            if (!options.containsKey("autoStopOnRecognize")) {
                options["autoStopOnRecognize"] = true
            }
            activeDetectOverlay = SunmiFaceDetectOverlay(activity, options, callbackOf(callback), true)
            activeDetectOverlay?.start()
            success(null, "start face recognize success")
        }
    }

    @JvmStatic
    fun startFaceDetectJson(optionsJson: String?, callback: ((String) -> Unit)?): String {
        return wrap {
            val activity = requireActivity()
            stopActiveDetectOverlay("replaced", "已替换上一层人脸识别", callback)
            val options = parseOptions(optionsJson)
            activeDetectOverlay = SunmiFaceDetectOverlay(activity, options, callbackOf(callback), false)
            activeDetectOverlay?.start()
            success(null, "start face detect success")
        }
    }

    @JvmStatic
    fun openFaceDetectJson(optionsJson: String?, callback: ((String) -> Unit)?): String {
        return wrap {
            val activity = requireActivity()
            stopActiveDetectOverlay("replaced", "已替换上一层人脸识别", callback)
            val options = parseOptions(optionsJson)
            if (!options.containsKey("autoStopOnRecognize")) {
                options["autoStopOnRecognize"] = true
            }
            activeDetectOverlay = SunmiFaceDetectOverlay(activity, options, callbackOf(callback), true)
            activeDetectOverlay?.start()
            success(null, "open face detect success")
        }
    }

    @JvmStatic
    fun stopFaceDetectJson(): String {
        return wrap {
            stopActiveDetectOverlay("stopped", "已关闭人脸识别", null)
            success(null, "stop face detect success")
        }
    }

    private fun callbackOf(callback: ((String) -> Unit)?): FaceDetectResultCallback? {
        if (callback == null) {
            return null
        }
        return FaceDetectResultCallback { json, _ ->
            callback.invoke(json)
        }
    }

    private fun stopActiveDetectOverlay(eventType: String, message: String, callback: ((String) -> Unit)?) {
        val overlay = activeDetectOverlay
        activeDetectOverlay = null
        overlay?.stop(eventType, message)
        if (overlay == null && callback != null && eventType == "replaced") {
            callback.invoke(success(null, message).toJSONString())
        }
    }

    private fun parseOptions(optionsJson: String?): JSONObject {
        return if (optionsJson.isNullOrEmpty()) JSONObject() else (JSON.parseObject(optionsJson) ?: JSONObject())
    }

    private fun requireActivity(): Activity {
        return UTSAndroid.getUniActivity() ?: throw IllegalStateException("activity is null")
    }

    private fun success(data: Any?, message: String): JSONObject {
        val result = JSONObject()
        result["code"] = 0
        result["success"] = true
        result["message"] = message
        result["data"] = data
        return result
    }

    private fun wrap(block: () -> JSONObject): String {
        return try {
            block().toJSONString()
        } catch (e: Exception) {
            val result = JSONObject()
            result["code"] = -1
            result["success"] = false
            result["message"] = e.message ?: e.toString()
            result["errorString"] = e.toString()
            result["data"] = null
            result.toJSONString()
        }
    }
}
