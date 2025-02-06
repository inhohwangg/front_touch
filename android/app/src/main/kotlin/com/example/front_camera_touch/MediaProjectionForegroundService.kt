package com.example.front_camera_touch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log

class MediaProjectionForegroundService : Service() {

    companion object {
        // 이 변수에 생성된 MediaProjection 인스턴스가 저장됩니다.
        var mediaProjection: MediaProjection? = null
        const val CHANNEL_ID = "MediaProjectionServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Foreground Service로 실행하기 위한 Notification 생성
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture Service")
                .setContentText("화면 캡처 기능이 실행 중입니다.")
                .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘 등의 유효한 리소스로 대체하세요.
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Screen Capture Service")
                .setContentText("화면 캡처 기능이 실행 중입니다.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        }
        // Android 14 이상에서도 적절한 타입(mediaProjection)으로 startForeground() 호출
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data!!)
            Log.d("MediaProjectionService", "MediaProjection이 정상적으로 생성되었습니다.")
        } catch (e: Exception) {
            Log.e("MediaProjectionService", "MediaProjection 생성 오류: ${e.message}")
        }
        // 서비스가 종료되면 자동 재시작하지 않음.
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Projection Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
