package com.example.test4

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class BatteryActivity : AppCompatActivity() {

    private lateinit var resultInfo: TextView

    private val ip = "192.168.0.103" //Retrofit
    private val port = "8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery)

        resultInfo = findViewById(R.id.result_info)

        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        Timer().schedule(timerTask {
            updateBatteryInfo()
        }, 0, 3000)
    }

    private fun updateBatteryInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val batteryResult = makeGetRequest("/bat") ?: return@launch
            withContext(Dispatchers.Main) {
                resultInfo.text = batteryResult.split("|").firstOrNull() ?: "Нет данных"
            }
        }
    }

    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("BatteryActivity", "Ошибка запроса $endpoint: ${e.message}")
            null
        }
    }
}