package com.example.test4

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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

class TemperatureActivity : AppCompatActivity() {

    private lateinit var tempInfo: TextView
    private lateinit var heaterImage: ImageView
    private var temperature = 0

    private val ip = "192.168.88.52"
    private val port = "8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        tempInfo = findViewById(R.id.temp_info)
        heaterImage = findViewById(R.id.heater_image)

        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        Timer().schedule(timerTask {
            updateTemperature()
        }, 0, 3000)

        findViewById<Button>(R.id.btn_plus).setOnClickListener { adjustTemperature(1) }
        findViewById<Button>(R.id.btn_minus).setOnClickListener { adjustTemperature(-1) }
    }

    private fun updateTemperature() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = makeGetRequest("/temp") ?: return@launch
            withContext(Dispatchers.Main) {
                tempInfo.text = response.split("|").firstOrNull() ?: "Нет данных"
            }
        }
    }

    private fun adjustTemperature(delta: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val newTemp = temperature + delta
            val response = makePostRequest("/temp?value=$newTemp")
            if (response != null) {
                withContext(Dispatchers.Main) {
                    tempInfo.text = newTemp.toString()
                    temperature = newTemp
                }
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