package com.max77.brightnessrange

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.reflect.Field
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private var minBrightness = 1_000_000
    private var maxBrightness = -1_000_000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()

        startBrightnessUpdates()
    }

    private var timer: Timer? = null

    private fun startBrightnessUpdates() {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val brightness = getScreenBrightness(this@MainActivity)
                minBrightness = min(minBrightness, brightness)
                maxBrightness = max(maxBrightness, brightness)

                runOnUiThread {
                    findViewById<TextView>(R.id.textView).text =
                        "PowerManager brightness range = ${getBrightnessRange()}\n" +
                                "Current brightness range = ($minBrightness, $maxBrightness)\n" +
                                "Current brightness = ${getScreenBrightness(this@MainActivity)}"
                }
            }
        }, 0, 100)
    }

    private fun getBrightnessRange(): Pair<Int, Int> {
        val min = getResInteger("config_screenBrightnessSettingMinimum")
        val max = getResInteger("config_screenBrightnessSettingMaximum")
        return Pair(min, max)
    }

    private fun getResInteger(name: String): Int {
        val id = resources.getIdentifier(name, "integer", "android")
        if (id != 0) {
            try {
                return resources.getInteger(id)
            } catch (e: Resources.NotFoundException) {
                // ignore
            }
        }
        return 0
    }

    private fun getMaxBrightness(context: Context, defaultValue: Int): Int {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        val fields: Array<Field> = powerManager.javaClass.declaredFields
        for (field in fields) {
            //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/os/PowerManager.java

            if (field.getName().equals("BRIGHTNESS_ON")) {
                field.isAccessible = true
                return try {
                    field.get(powerManager) as Int
                } catch (e: IllegalAccessException) {
                    defaultValue
                }
            }
        }
        return defaultValue
    }

    private fun getScreenBrightness(context: Context): Int {
        try {
//            Settings.System.putInt(
//                contentResolver,
//                Settings.System.SCREEN_BRIGHTNESS_MODE,
//                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
//            )

            return Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: SettingNotFoundException) {
            Log.e("Error", "Cannot access system brightness")
            e.printStackTrace()
            return -1
        }
    }
}