package ru.spb.dair.roadmovie

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.support.v4.app.NotificationManagerCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.location.Criteria.ACCURACY_HIGH
import android.location.LocationManager.GPS_PROVIDER
import android.media.RingtoneManager
import android.os.*
import android.support.v4.app.NotificationCompat.CATEGORY_ALARM
import ru.spb.dair.roadmovie.MyService.LocalBinder
import android.os.IBinder




class MyService : Service(), LocationListener {

    val MIN_SPEED = 20 / 3.6
    val MAX_TIME = 10 * 60 * 1000

    var _locationManager: LocationManager? = null
    var _lastLocationTime: Long = 0
    var _totalTimePassed: Long = 0
    public var userNotified = false
    var _notificationId: Int = 0

    inner class LocalBinder : Binder() {
        internal val service: MyService
            get() = this@MyService
    }

    private val mBinder = LocalBinder()


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _lastLocationTime = 0
        userNotified = false

        createNotificationChannel()

        _locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (_locationManager != null) {
            _locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 0.0, this)
        }

//        Handler().postDelayed({
//            notifyTheUser()
//        }, 2000)

        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        // Called when a new location is found by the network location provider.

        if (location.speed < MIN_SPEED) {
            return
        }

        if (_lastLocationTime == 0L) {
            _lastLocationTime = location.time
            _totalTimePassed = 0L
            userNotified = false
        }
        else {
            val delta = location.time - _lastLocationTime
            _totalTimePassed += delta

            if (!userNotified && _totalTimePassed > MAX_TIME) {
                notifyTheUser()
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "RoadMovie notification channel"
            val description = "To notify the user about timout"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ru.spb.dair.RoadMovie", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


    private fun notifyTheUser() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)


        val mBuilder = NotificationCompat.Builder(this, "ru.spb.dair.RoadMovie")
                .setContentTitle("Road Movie")
                .setContentText("Пассажира пора высаживать")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setAutoCancel(false)
                .setCategory(CATEGORY_ALARM)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        val notificationManager = NotificationManagerCompat.from(this)

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(_notificationId++, mBuilder.build())

        userNotified = true
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

}
