package ru.spb.dair.roadmovie

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Context.ACTIVITY_SERVICE
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import android.content.ComponentName
import ru.spb.dair.roadmovie.MyService.LocalBinder
import android.os.IBinder
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    lateinit var _mainButton: Button
    lateinit var _unloadLabel: TextView
    lateinit var _speedLabel: TextView
    lateinit var _timePassed: TextView

    private var mBoundService: MyService? = null
    private var mShouldUnbind: Boolean = false

    private lateinit var timer: Timer

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = (service as MyService.LocalBinder).service
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null
        }
    }

    fun doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        if (bindService(Intent(this@MainActivity, MyService::class.java),
                        mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true
        }
    }

    fun doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(mConnection)
            mShouldUnbind = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _mainButton = this.findViewById<Button>(R.id.main_button)
        _mainButton.setOnClickListener {
            onButtonClick()
        }

        _unloadLabel = this.findViewById(R.id.unloadLabel)
        _unloadLabel.visibility = INVISIBLE

        _speedLabel = this.findViewById(R.id.lastSpeed)
        _speedLabel.text = ""

        _timePassed = this.findViewById(R.id.timePassed)
        _timePassed.text = ""
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 42);
        }

        if (isMyServiceRunning(MyService::class.java)) {
            doBindService()
        }

        updateButtonText()

        timer = Timer("updateTimer")
        timer.scheduleAtFixedRate(1000, 1000, {
            runOnUiThread {
                updateButtonText()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                // whatever
            }
        }
    }


    override fun onPause() {
        timer.cancel()
        if (isMyServiceRunning(MyService::class.java)) {
            doUnbindService()
        }
        super.onPause()
    }

    private fun updateButtonText() {
        if (isMyServiceRunning(MyService::class.java)) {
            _mainButton.text = "Остановить поездку"
            if (mBoundService != null) {
                val unload = mBoundService!!.userNotified
                if (unload) {
                    _unloadLabel.visibility = VISIBLE
                }
                else {
                    _unloadLabel.visibility = INVISIBLE
                }

                _speedLabel.text = (mBoundService!!.lastSpeedFound * 3.6).toString()

                var secPassed = mBoundService!!._totalTimePassed / 1000
                val minPassed = secPassed / 60
                secPassed = secPassed % 60

                _timePassed.text = String.format("%02d:%02d", minPassed, secPassed)
            }
        }
        else {
            _mainButton.text = "Начать поездку"
            _unloadLabel.visibility = INVISIBLE
            _speedLabel.text = ""
            _timePassed.text = ""
        }
    }

    private fun onButtonClick() {
        val intent = Intent(this, MyService::class.java)
        if (!isMyServiceRunning(MyService::class.java)) {
            startService(intent)
            doBindService()
        }
        else {
            mBoundService?.userNotified = false
            doUnbindService()
            stopService(intent)
        }

        updateButtonText()
    }
}
