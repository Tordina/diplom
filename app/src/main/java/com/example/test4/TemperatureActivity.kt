package com.example.test4

import android.annotation.SuppressLint
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
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Switch
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONObject

class TemperatureActivity : AppCompatActivity() {

    private lateinit var tempInfo: TextView
    private lateinit var heaterImage: ImageView
    private lateinit var btnHeaterOn: Button
    private lateinit var btnHeaterOff: Button
    private var currentMode = "manual"

    private lateinit var progressBar: ProgressBar

    private val ip = "172.20.10.4"
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
        progressBar = findViewById(R.id.progress_bar)
        btnHeaterOn = findViewById(R.id.btn_heater_on)
        btnHeaterOff = findViewById(R.id.btn_heater_off)
        updateTemperature()
        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }
        progressBar = findViewById(R.id.progress_bar)

        // Показываем ProgressBar при запуске
        progressBar.visibility = View.VISIBLE

        Timer().schedule(timerTask {
            updateTemperature()
        }, 0, 10000)
        findViewById<Switch>(R.id.mode_switch).setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Показываем ProgressBar перед началом операции
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.VISIBLE
                        findViewById<Switch>(R.id.mode_switch).isEnabled = false // Отключаем Switch на время операции
                    }

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
                    // В случае ошибки возвращаем Switch в предыдущее состояние
                    withContext(Dispatchers.Main) {
                        findViewById<Switch>(R.id.mode_switch).isChecked = !isChecked
                    }
                } finally {
                    delay(15000) // Минимальное время показа ProgressBar
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        findViewById<Switch>(R.id.mode_switch).isEnabled = true
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_heater_on).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch { // Запускаем в Main потоке для UI операций
                try {
                    // Показываем ProgressBar и блокируем кнопки
                    progressBar.visibility = View.VISIBLE
                    btnHeaterOn.isEnabled = false
                    btnHeaterOff.isEnabled = false

                    // Выполняем сетевой запрос в IO потоке
                    val response = withContext(Dispatchers.IO) {
                        makeGetRequest("/heater_on")
                    }

                    if (response != null) {
                        updateButtonColors(true)
                        // Обновляем данные с сервера
                        updateTemperature()
                    }
                } catch (e: Exception) {
                    Log.e("TemperatureActivity", "Ошибка включения обогрева: ${e.message}")
                } finally {
                    // Всегда скрываем ProgressBar и разблокируем кнопки
                    delay(3000)
                    progressBar.visibility = View.GONE
                    btnHeaterOn.isEnabled = true
                    btnHeaterOff.isEnabled = true
                }
            }
        }

        findViewById<Button>(R.id.btn_heater_off).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    progressBar.visibility = View.VISIBLE
                    btnHeaterOn.isEnabled = false
                    btnHeaterOff.isEnabled = false

                    val response = withContext(Dispatchers.IO) {
                        makeGetRequest("/heater_off")
                    }

                    if (response != null) {
                        updateButtonColors(false)
                        updateTemperature()
                    }
                } catch (e: Exception) {
                    Log.e("TemperatureActivity", "Ошибка выключения обогрева: ${e.message}")
                } finally {
                    delay(3000)
                    progressBar.visibility = View.GONE
                    btnHeaterOn.isEnabled = true
                    btnHeaterOff.isEnabled = true
                }
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
            delay(2000) // для прогресс бара

            try {
                showLoading(true)
                val response = makeGetRequest("/temp")

                response?.let {
                    val json = JSONObject(it)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        currentMode = json.getString("mode")
                        tempInfo.text = "${json.getInt("temperature")}°C"
                        findViewById<Switch>(R.id.mode_switch).isChecked = currentMode == "auto"

                        val heaterOn = json.getBoolean("heater_on")
                        heaterImage.setImageResource(
                            if (heaterOn) R.drawable.heater_on else R.drawable.heater_off
                        )
                        updateButtonColors(heaterOn) // Обновляем цвета кнопок
                        heaterImage.visibility = View.VISIBLE
                        updateUI()
                    }
                }
            } catch (e: Exception) {
                Log.e("TemperatureActivity", "Ошибка: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    private fun showLoading(show: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            findViewById<Switch>(R.id.mode_switch).isEnabled = !show
        }
    }
    private fun updateButtonColors(heaterOn: Boolean) {
        if (heaterOn) {
            btnHeaterOn.setBackgroundColor(ContextCompat.getColor(this, R.color.button_active))
            btnHeaterOff.setBackgroundColor(ContextCompat.getColor(this, R.color.button_default))
        } else {
            btnHeaterOn.setBackgroundColor(ContextCompat.getColor(this, R.color.button_default))
            btnHeaterOff.setBackgroundColor(ContextCompat.getColor(this, R.color.button_active))
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