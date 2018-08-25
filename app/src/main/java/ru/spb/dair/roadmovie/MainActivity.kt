package ru.spb.dair.roadmovie

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Context.ACTIVITY_SERVICE
import android.app.ActivityManager
import android.content.Context
import android.content.Intent


class MainActivity : AppCompatActivity() {

    lateinit var _mainButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _mainButton = this.findViewById<Button>(R.id.main_button)
        _mainButton.setOnClickListener {
            onButtonClick()
        }
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

        updateButtonText()
    }

    private fun updateButtonText() {
        if (isMyServiceRunning(MyService::class.java)) {
            _mainButton.text = "Остановить поездку"
        }
        else {
            _mainButton.text = "Начать поездку"
        }
    }

    private fun onButtonClick() {
        val intent = Intent(this, MyService::class.java)
        if (!isMyServiceRunning(MyService::class.java)) {
            startService(intent)
        }
        else {
            stopService(intent)
        }

        updateButtonText()
    }
}
