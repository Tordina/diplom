package com.example.test4

import android.animation.ObjectAnimator
import android.app.VoiceInteractor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.tools.core.model.Method
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class WaterActivity : AppCompatActivity() {
    private lateinit var bakImage: ImageView
    private lateinit var waterLevelText: TextView
    private lateinit var refreshButton: Button
    private lateinit var backButton: Button
    private val ip = "192.168.0.103"
    private val port = "8000"
    private var lastWaterLevel = 0
    private var updateTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water)

        bakImage = findViewById(R.id.bak_image)
        waterLevelText = findViewById(R.id.water_level_text)
        refreshButton = findViewById(R.id.refresh_btn)
        backButton = findViewById(R.id.back_btn)

        backButton.setOnClickListener {
            finish()  // Возврат на предыдущую активность
        }
        refreshButton.setOnClickListener { fetchWaterLevel() }
        bakImage.isClickable = false

        // Автообновление каждые 3 секунды
        startAutoUpdate()
        fetchWaterLevel()
    }

    private fun startAutoUpdate() {
        updateTimer = Timer()
        updateTimer?.schedule(timerTask {
            fetchWaterLevel()
        }, 0, 3000)
    }

    private fun fetchWaterLevel() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = makeGetRequest("/water")
                response?.let {
                    val json = JSONObject(it)
                    val level = json.getInt("water_level")

                    // Сохраняем успешное значение
                    lastWaterLevel = level

                    runOnUiThread {
                        updateUI(level)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    // Используем последнее успешное значение вместо ошибки
                    updateUI(lastWaterLevel)
                    Log.w("WaterActivity", "Используем кэшированное значение: $lastWaterLevel%")
                }
            }
        }
    }

    private fun updateUI(level: Int) {
        waterLevelText.text = "Уровень воды: ${level}%"
        val resId = if (level > 0) R.drawable.bak_full else R.drawable.bak_empty
        bakImage.setImageResource(resId)

    }

    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("WaterActivity", "Ошибка запроса: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        updateTimer?.cancel()
        super.onDestroy()
    }
}