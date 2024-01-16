package au.edu.utas.ywang150.kit721assignment2.ui.modules

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.*

/**
 * Alarm module: Customized feature
 *
 * User can either turn on or off the alarm by switching the button on home fragment,
 * it provides a daily alarm for waking up the baby based on the data recorded,
 * the wake up time is the average end time of sleeping events
 *
 * */
class AlarmModule : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Get the default alarm sound
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        // Set up a MediaPlayer to play the alarm sound (From ChatGPT)
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(context, alarmUri)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            setAudioAttributes(audioAttributes)
            setVolume(AudioManager.STREAM_ALARM.toFloat(), AudioManager.STREAM_ALARM.toFloat())
            isLooping = true
            prepare()
        }

        // Play the alarm sound
        mediaPlayer.start()

        // Stop the alarm sound after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            mediaPlayer.stop()
            mediaPlayer.release()
        }, 10000)
    }

    // Enable a daily alarm based on the scheduled time
    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int, second: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmModule::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for the next day
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val interval = AlarmManager.INTERVAL_DAY
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, interval, pendingIntent)
    }

    // Disable the alarm
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmModule::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        alarmManager.cancel(pendingIntent)
    }
}