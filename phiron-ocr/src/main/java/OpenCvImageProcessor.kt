package uts.sdk.modules.phironocr

import android.graphics.Bitmap
import android.graphics.Matrix
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object OpenCvImageProcessor {
    private var initialized = false

    fun ensureOpenCvReady() {
        if (!initialized) {
            initialized = OpenCVLoader.initDebug()
            if (!initialized) {
                throw IllegalStateException("OpenCV init failed")
            }
        }
    }

    fun preprocess(bitmap: Bitmap, options: JSONObject): ProcessedImageResult {
        ensureOpenCvReady()
        var working = bitmapToMat(bitmap)
        val metadata = JSONObject()

        options.getJSONObject("cropRoi")?.let { roi ->
            working = crop(working, roi)
            metadata["roiApplied"] = roi
        }

        if (options.getBooleanValue("enablePerspectiveCorrection")) {
            val points = options.getJSONArray("perspectivePoints")
            if (points != null && points.size >= 4) {
                working = perspectiveTransform(working, points)
                metadata["perspectiveCorrected"] = true
            }
        }

        val rotateDegrees = options.getDoubleValue("manualRotateDegrees")
        if (abs(rotateDegrees) > 0.0001) {
            working = rotate(working, rotateDegrees)
            metadata["manualRotateDegrees"] = rotateDegrees
        }

        if (options.getBooleanValue("enableDeskew")) {
            val deskewResult = deskew(working)
            working = deskewResult.first
            metadata["deskewAngle"] = deskewResult.second
        }

        if (options.getBooleanValue("enableGray")) {
            working = toGray(working)
            metadata["gray"] = true
        }

        if (options.getBooleanValue("enableDenoise")) {
            val kernel = normalizeOddKernel(options.getIntValue("denoiseKernelSize"), 3)
            val denoised = Mat()
            Imgproc.medianBlur(working, denoised, kernel)
            working.release()
            working = denoised
            metadata["denoiseKernelSize"] = kernel
        }

        if (options.getBooleanValue("enableContrast")) {
            val alpha = options.getDoubleValue("contrastAlpha").let { if (it == 0.0) 1.45 else it }
            val beta = options.getDoubleValue("contrastBeta")
            val contrasted = Mat()
            working.convertTo(contrasted, -1, alpha, beta)
            working.release()
            working = contrasted
            metadata["contrast"] = JSONObject().apply {
                this["alpha"] = alpha
                this["beta"] = beta
            }
        }

        if (options.getBooleanValue("enableSharpen")) {
            val sigma = options.getDoubleValue("sharpenSigma").let { if (it <= 0.0) 1.2 else it }
            working = sharpen(working, sigma)
            metadata["sharpenSigma"] = sigma
        }

        if (options.getBooleanValue("enableAdaptiveThreshold")) {
            if (working.channels() > 1) {
                working = toGray(working)
            }
            val blockSize = normalizeOddKernel(options.getIntValue("adaptiveBlockSize"), 31)
            val cValue = options.getDoubleValue("adaptiveC").let { if (it == 0.0) 12.0 else it }
            val thresholded = Mat()
            Imgproc.adaptiveThreshold(
                working,
                thresholded,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                blockSize,
                cValue
            )
            working.release()
            working = thresholded
            metadata["adaptiveThreshold"] = JSONObject().apply {
                this["blockSize"] = blockSize
                this["c"] = cValue
            }
        }

        val resultBitmap = matToBitmap(working)
        metadata["width"] = resultBitmap.width
        metadata["height"] = resultBitmap.height
        working.release()
        return ProcessedImageResult(resultBitmap, metadata)
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val argbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val mat = Mat()
        Utils.bitmapToMat(argbBitmap, mat)
        return mat
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val converted = if (mat.type() == CvType.CV_8UC1) {
            val rgba = Mat()
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA)
            rgba
        } else {
            val rgba = Mat()
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_BGR2RGBA)
            rgba
        }
        val bitmap = Bitmap.createBitmap(converted.cols(), converted.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(converted, bitmap)
        converted.release()
        return bitmap
    }

    private fun crop(src: Mat, roi: JSONObject): Mat {
        val left = max(0, roi.getIntValue("left"))
        val top = max(0, roi.getIntValue("top"))
        val width = max(1, roi.getIntValue("width"))
        val height = max(1, roi.getIntValue("height"))
        val safeWidth = min(width, src.cols() - left)
        val safeHeight = min(height, src.rows() - top)
        val rect = Rect(left, top, safeWidth, safeHeight)
        val cropped = Mat(src, rect).clone()
        src.release()
        return cropped
    }

    private fun perspectiveTransform(src: Mat, points: JSONArray): Mat {
        val srcPoints = pointsToMat(points)
        val ordered = orderPoints(points)
        val widthTop = distance(ordered[0], ordered[1])
        val widthBottom = distance(ordered[2], ordered[3])
        val maxWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(1)
        val heightLeft = distance(ordered[0], ordered[3])
        val heightRight = distance(ordered[1], ordered[2])
        val maxHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(1)
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )
        val matrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val output = Mat()
        Imgproc.warpPerspective(src, output, matrix, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        src.release()
        srcPoints.release()
        dstPoints.release()
        matrix.release()
        return output
    }

    private fun rotate(src: Mat, degrees: Double): Mat {
        val center = Point(src.cols() / 2.0, src.rows() / 2.0)
        val rotation = Imgproc.getRotationMatrix2D(center, degrees, 1.0)
        val bbox = Size(src.cols().toDouble(), src.rows().toDouble())
        val output = Mat()
        Imgproc.warpAffine(src, output, rotation, bbox, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(255.0, 255.0, 255.0, 255.0))
        src.release()
        rotation.release()
        return output
    }

    private fun toGray(src: Mat): Mat {
        if (src.channels() == 1) return src
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        src.release()
        return gray
    }

    private fun sharpen(src: Mat, sigma: Double): Mat {
        val blurred = Mat()
        Imgproc.GaussianBlur(src, blurred, Size(0.0, 0.0), sigma)
        val output = Mat()
        Core.addWeighted(src, 1.7, blurred, -0.7, 0.0, output)
        src.release()
        blurred.release()
        return output
    }

    private fun deskew(src: Mat): Pair<Mat, Double> {
        val gray = if (src.channels() == 1) src.clone() else toGray(src.clone())
        val binary = Mat()
        Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
        val points = MatOfPoint()
        Core.findNonZero(binary, points)
        if (points.empty()) {
            gray.release()
            binary.release()
            points.release()
            return Pair(src, 0.0)
        }
        val mat2f = MatOfPoint2f(*points.toArray())
        val rect = Imgproc.minAreaRect(mat2f)
        var angle = rect.angle
        if (angle < -45) {
            angle += 90.0
        }
        gray.release()
        binary.release()
        points.release()
        mat2f.release()
        return Pair(rotate(src, angle), angle)
    }

    private fun pointsToMat(points: JSONArray): MatOfPoint2f {
        val list = ArrayList<Point>()
        for (i in 0 until min(points.size, 4)) {
            val point = points.getJSONObject(i)
            list.add(Point(point.getDoubleValue("x"), point.getDoubleValue("y")))
        }
        return MatOfPoint2f(*list.toTypedArray())
    }

    private fun orderPoints(points: JSONArray): Array<Point> {
        val list = ArrayList<Point>()
        for (i in 0 until min(points.size, 4)) {
            val point = points.getJSONObject(i)
            list.add(Point(point.getDoubleValue("x"), point.getDoubleValue("y")))
        }
        val sortedBySum = list.sortedBy { it.x + it.y }
        val sortedByDiff = list.sortedBy { it.y - it.x }
        return arrayOf(
            sortedBySum.first(),
            sortedByDiff.first(),
            sortedBySum.last(),
            sortedByDiff.last()
        )
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun normalizeOddKernel(value: Int, fallback: Int): Int {
        val safe = if (value <= 1) fallback else value
        return if (safe % 2 == 0) safe + 1 else safe
    }
}

data class ProcessedImageResult(
    val bitmap: Bitmap,
    val metadata: JSONObject
)
