// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.settings.SettingFlag
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CHANNEL = "robust_mode"
private const val NOTIFICATION_ID = 1
private const val ACTION_TURN_OFF = "com.qtekfun.ultimatedeck.TURN_OFF_ROBUST_MODE"
private val BEAT = 30.minutes

/**
 * Robust mode (RF-10): a foreground service whose only job is to keep the process alive on
 * phones that kill apps in the background (ColorOS, MIUI, OriginOS…), where the alarms of a
 * killed app are lost. Every [BEAT] it runs [ReminderBeat]; it never uses the network. Its
 * fixed notification says what it is for and turns the mode off.
 */
@AndroidEntryPoint
class KeepAliveService : Service() {
    @Inject
    lateinit var beat: ReminderBeat

    @Inject
    lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TURN_OFF) {
            // The controller sees the setting change too; stopping here is just quicker.
            settings.setFlag(SettingFlag.ROBUST_MODE, false)
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), foregroundType())
        if (loop?.isActive != true) {
            loop = scope.launch {
                while (isActive) {
                    delay(BEAT)
                    beat.beat()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL,
            getString(R.string.robust_channel),
            NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(getString(R.string.robust_title))
        .setContentText(getString(R.string.robust_text))
        .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.robust_text)))
        .setOngoing(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0,
            getString(R.string.robust_turn_off),
            PendingIntent.getService(
                this,
                1,
                Intent(this, KeepAliveService::class.java).setAction(ACTION_TURN_OFF),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    // Android 14 asks for the type when starting; before it the manifest's is enough.
    private fun foregroundType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
}
