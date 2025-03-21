package com.example.test4

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import org.json.JSONObject

class TemperatureActivity : AppCompatActivity() {

    private lateinit var tempInfo: TextView
    private lateinit var heaterImage: ImageView
    private var currentMode = "manual"
    private var isAdjusting = false

    private val ip = "192.168.0.103"
    private val port = "8000"

    private lateinit var manualControls: LinearLayout
    private lateinit var autoControls: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)
        manualControls = findViewById(R.id.manual_controls)
        autoControls = findViewById(R.id.auto_controls)
        tempInfo = findViewById(R.id.temp_info)
        heaterImage = findViewById(R.id.heater_image)
        updateTemperature()
        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        Timer().schedule(timerTask {
            updateTemperature()
        }, 0, 3000)

        findViewById<Button>(R.id.btn_plus).setOnClickListener { adjustTemperature(1) }
        findViewById<Button>(R.id.btn_minus).setOnClickListener { adjustTemperature(-1) }

        // Замените блок с Switch на этот код
        findViewById<Switch>(R.id.mode_switch).setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val newMode = if (isChecked) "auto" else "manual"
                    val target = if (isChecked) {
                        withContext(Dispatchers.Main) {
                            findViewById<EditText>(R.id.target_input).text.toString()
                        }
                    } else ""

                    val response = makePostRequest(
                        if (isChecked) "/set_mode?mode=auto&target=$target"
                        else "/set_mode?mode=manual"
                    )

                    if (response?.contains("updated") == true) {
                        currentMode = newMode
                        withContext(Dispatchers.Main) {
                            updateUI()
                            updateTemperature()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TemperatureActivity", "Ошибка смены режима: ${e.message}")
                }
            }

        }
        findViewById<Button>(R.id.btn_heater_on).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                makeGetRequest("/heater_on")
            }
        }
        findViewById<Button>(R.id.btn_heater_off).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                makeGetRequest("/heater_off")
            }
        }
        // Установка целевой температуры
        findViewById<Button>(R.id.btn_set_target).setOnClickListener {
            val target = findViewById<EditText>(R.id.target_input).text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                makePostRequest("/set_mode?mode=auto&target=$target")
            }
        }
    }
    private fun updateUI() {
        runOnUiThread {
            manualControls.visibility = if (currentMode == "manual") View.VISIBLE else View.GONE
            autoControls.visibility = if (currentMode == "auto") View.VISIBLE else View.GONE
        }
    }

    private fun updateTemperature() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = makeGetRequest("/temp") ?: return@launch
            try {
                val json = JSONObject(response)
                withContext(Dispatchers.Main) {
                    // Обновляем все параметры с сервера
                    currentMode = json.getString("mode")
                    tempInfo.text = "${json.getInt("temperature")}°C"
                    findViewById<Switch>(R.id.mode_switch).isChecked = currentMode == "auto"

                    // Обновление обогревателя для всех режимов
                    heaterImage.setImageResource(
                        if (json.getBoolean("heater_on"))
                            R.drawable.heater_on
                        else R.drawable.heater_off
                    )
                    heaterImage.visibility = View.VISIBLE

                    // Синхронизация UI
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e("TemperatureActivity", "Ошибка: ${e.message}")
            }
        }
    }

    private fun adjustTemperature(delta: Int) {
        if (isAdjusting || currentMode == "auto") return
        isAdjusting = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentTemp = tempInfo.text.toString()
                    .replace("°C", "").toInt()
                val newTemp = currentTemp + delta

                val response = makePostRequest("/temp?value=$newTemp")
                if (response?.contains("updated") == true) {
                    updateTemperature() // Принудительное обновление
                }
            } finally {
                isAdjusting = false
            }
        }
    }


    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("TemperatureActivity", "Ошибка запроса $endpoint: ${e.message}")
            null
        }
    }

    private fun makePostRequest(endpoint: String): String? {
        return try {
            val url = URL("http://$ip:$port$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connect()
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            response
        } catch (e: Exception) {
            Log.e("TemperatureActivity", "Ошибка POST запроса $endpoint: ${e.message}")
            null
        }
    }
}