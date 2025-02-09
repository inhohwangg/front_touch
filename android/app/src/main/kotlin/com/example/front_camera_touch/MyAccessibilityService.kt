package com.example.front_camera_touch

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

class MyAccessibilityService : AccessibilityService() {
    companion object {
        // MainActivity에서 할당한 MediaProjection 객체를 저장할 변수
        var mediaProjection: MediaProjection? = null
    }
    private lateinit var overlayView: View
    private lateinit var windowManager: WindowManager
    private var screenWidth = 0
    private var screenHeight = 0
    private var isOverlayAdded = false
    private var lastTouchTime = 0L

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionManager: MediaProjectionManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // setupOverlay()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        Handler(Looper.getMainLooper()).post {
            setupOverlay()
        }
    }

    private fun setupOverlay() {
        if (isOverlayAdded) return  // 중복 실행 방지
        isOverlayAdded = true

        val cameraWidth = 200
        val cameraHeight = 200

        // windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            cameraWidth,
            cameraHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // 필요에 따라 y offset을 조정 (예: 100픽셀 아래부터 시작)
            y = 0
        }


        overlayView = View(this).apply {
            setBackgroundColor(0x00000000) // 투명 배경
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX
                    val y = event.rawY
                    Log.d("AccessibilityService", "터치 감지: ($x, $y)")

                    if (isFrontCameraTouched(x, y)) {
                        takeScreenshot()
                        return@setOnTouchListener true  // 영역 내에서는 이벤트 소비
                    }
                }
                false
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e("AccessibilityService", "오버레이 추가 실패: ${e.message}")
        }
    }

    // private fun isFrontCameraTouched(x: Float, y: Float): Boolean {
    //     // 전면 카메라 영역을 대략적으로 설정 (예: 상단 중앙)
    //     val screenWidth = resources.displayMetrics.widthPixels
    //     val screenHeight = resources.displayMetrics.heightPixels
    //     val cameraAreaX = screenWidth / 2 - 100
    //     val cameraAreaY = 100

    //     return (x.toInt() in cameraAreaX..(cameraAreaX + 200) &&
    //         y.toInt() in 0..(cameraAreaY + 200))
    // }
    private fun isFrontCameraTouched(x: Float, y: Float): Boolean {
        val cameraAreaX = screenWidth / 2 - 100
        val cameraAreaY = 100

        return (x.toInt() in cameraAreaX..(cameraAreaX + 200) &&
            y.toInt() in 0..(cameraAreaY + 200))
    }

    private fun takeScreenshot() {
        Thread {
            try {
                Log.d("AccessibilityService", "스크린샷 촬영 시작")
                val projection = MediaProjectionForegroundService.mediaProjection
                if (projection == null) {
                    Log.e("AccessibilityService", "MediaProjection is null. 스크린샷 권한 필요")
                    return@Thread
                }
                Log.d("AccessibilityService", "MediaProjection 사용 가능: $projection")
                val density = resources.displayMetrics.densityDpi

                // ImageReader 및 VirtualDisplay 생성
                imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
                val virtualDisplay = projection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )
                Log.d("AccessibilityService", "VirtualDisplay 생성됨")

                // 약간의 지연 후 이미지 획득 (500ms 정도)
                Thread.sleep(500)

                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    Log.d("AccessibilityService", "스크린샷 캡처 완료")
                    // Image를 Bitmap으로 변환
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth
                    // Bitmap의 넓이는 보통 rowStride와 관련 있음. 필요에 따라 정확한 크기로 자를 수 있음.
                    var bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    // 정확한 화면 사이즈로 자르기 (선택 사항)
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)

                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    // Bitmap을 파일로 저장 (예시)
                    val file = File(downloadsDir, "screenshot_${System.currentTimeMillis()}.png")
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    Log.d("AccessibilityService", "스크린샷 파일 저장 완료: ${file.absolutePath}")

                    image.close()
                } else {
                    Log.e("AccessibilityService", "이미지 캡처 실패")
                }
                imageReader?.close()
                virtualDisplay.release()
            } catch (e: Exception) {
                Log.e("AccessibilityService", "스크린샷 오류: ${e.message}")
            }
        }.start()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
