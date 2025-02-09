package com.example.front_camera_touch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    private val SCREEN_CAPTURE_REQUEST_CODE = 1001
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // 사용자에게 화면 캡처 권한 요청 인텐트 실행
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            SCREEN_CAPTURE_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            Log.d("MainActivity","onActivityResult : resultCode = $resultCode, data = $data")
            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d("MainActivity", "스크린 캡처 권한 승인 완료")
                // Foreground Service 시작
                val serviceIntent = Intent(this, MediaProjectionForegroundService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startForegroundService(serviceIntent)
                Log.d("MainActivity", "MediaProjectionForegroundService가 시작되었습니다.")
            } else {
                Log.e("MainActivity", "스크린 캡처 권한이 거부되었습니다.")
            }
        }
    }
}
