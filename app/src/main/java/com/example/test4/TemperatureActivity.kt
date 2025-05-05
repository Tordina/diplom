package com.example.test4

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
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat

class TemperatureActivity : AppCompatActivity() {

    private lateinit var tempInfo: TextView
    private lateinit var heaterImage: ImageView
    private lateinit var btnHeaterOn: Button
    private lateinit var btnHeaterOff: Button
    private var currentMode = "manual"
    private lateinit var progressBar: ProgressBar
    private lateinit var ip: String
    private lateinit var port: String
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
        progressBar = findViewById(R.id.progress_bar)
        ip = getString(R.string.server_ip)
        port = getString(R.string.server_port)
        val backButton: Button = findViewById(R.id.back_btn)

        backButton.setOnClickListener {
            finish()
        }

        progressBar.visibility = View.VISIBLE

        Timer().schedule(timerTask {
            updateTemperature()
        }, 0, 3000)


        findViewById<Button>(R.id.btn_heater_on).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    withContext(Dispatchers.IO) {
                        makeGetRequest("/heater_on")
                    }
                    updateButtonColors(true)

                }
                catch (e: Exception) {
                    Log.e("TemperatureActivity", "Ошибка включения обогрева: ${e.message}")
                }
            }
        }

        findViewById<Button>(R.id.btn_heater_off).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    withContext(Dispatchers.IO) {
                        makeGetRequest("/heater_off")
                    }
                    updateButtonColors(false)

                }
                catch (e: Exception) {
                    Log.e("TemperatureActivity", "Ошибка выключения обогрева: ${e.message}")
                }
            }
        }

        findViewById<Switch>(R.id.mode_switch).setOnClickListener {
            val pos = findViewById<Switch>(R.id.mode_switch).isChecked

            if (pos) {
                currentMode = "auto"
            }
            else {
                currentMode = "manual"
                CoroutineScope(Dispatchers.IO).launch {
                    makeGetRequest("/heater_off")
                }
            }

            updateUI()
        }

        findViewById<Button>(R.id.btn_set_target).setOnClickListener {
            val input = findViewById<EditText>(R.id.target_input).text.toString()
            val target = input.toDoubleOrNull() ?: run {
                Toast.makeText(this, "Некорректное число", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
                }

            CoroutineScope(Dispatchers.IO).launch {
                makeGetRequest("/set_mode?target=$target")
            }
        }
    }

    private fun updateUI() {
        runOnUiThread {
            manualControls.visibility = if (currentMode == "manual") View.VISIBLE else View.GONE
            autoControls.visibility = if (currentMode == "auto") View.VISIBLE else View.GONE
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
            btnHeaterOn.setBackgroundColor(ContextCompat.getColor(this, R.color.accentWarm))
            btnHeaterOff.setBackgroundColor(ContextCompat.getColor(this, R.color.translucentDark))
        }
        else {
            btnHeaterOn.setBackgroundColor(ContextCompat.getColor(this, R.color.translucentDark))
            btnHeaterOff.setBackgroundColor(ContextCompat.getColor(this, R.color.accentWarm))
        }
    }

    private fun updateTemperature() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showLoading(true)
                val response = makeGetRequest("/temp")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tempInfo.text = response
                    heaterImage.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("TemperatureActivity", "Ошибка при получении температуры: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tempInfo.text = "Ошибка подключения"
                }
            } finally {
                showLoading(false)
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

}