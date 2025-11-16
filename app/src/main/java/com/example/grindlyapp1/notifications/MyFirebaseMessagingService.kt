package com.example.grindlyapp1.notifications

import android.content.Context
import android.content.SharedPreferences
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.grindlyapp1.MainActivity
import com.example.grindlyapp1.R
import com.example.grindlyapp1.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", null)

        if (userId != null) {
            sendTokenToServer(userId, token)
        } else {
            Log.e("FCM", "User ID not found in SharedPreferences")
        }


    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received: $remoteMessage")

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true) // default true

        if (!notificationsEnabled) {
            Log.d("FCM", "Notifications disabled. Skipping display.")
            return
        }

        // 1️⃣ Handle notification payload (usually for background messages)
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Grindly"
            val body = notification.body ?: "You have a new notification"
            sendNotification(title, body)
        }

        // 2️⃣ Handle data payload (works in foreground)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Grindly"
            val body = remoteMessage.data["body"] ?: "You have a new notification"
            sendNotification(title, body)
            Log.d("FCM", "Data payload handled: $title - $body")
        }
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "grindly_channel"

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Grindly Notifications",
                NotificationManager.IMPORTANCE_HIGH // High importance ensures pop-up
            )
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.description = "Channel for Grindly app notifications"
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title ?: "Grindly")
            .setContentText(messageBody ?: "You have a new notification")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For pre-Oreo devices
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Vibration, lights, sound

        val notificationId = System.currentTimeMillis().toInt() // Unique ID
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("FCM", "Notification sent: $title - $messageBody")
    }


    private fun sendTokenToServer(userId: String, token: String) {
        val api = RetrofitClient.getClient(applicationContext)
        api.updateFcmToken(mapOf("userId" to userId, "token" to token))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "Token sent to backend successfully")
                    } else {
                        Log.e("FCM", "Failed to send token: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCM", "Error sending token to backend", t)
                }
            })
    }



}
