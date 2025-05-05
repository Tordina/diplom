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
import java.net.URL

class WaterActivity : AppCompatActivity() {
    private lateinit var bakImage: ImageView
    private lateinit var waterLevelText: TextView
    private lateinit var refreshButton: Button
    private lateinit var backButton: Button
    private lateinit var ip: String
    private lateinit var port: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water)

        ip = getString(R.string.server_ip)
        port = getString(R.string.server_port)
        bakImage = findViewById(R.id.bak_image)
        waterLevelText = findViewById(R.id.result_info)
        refreshButton = findViewById(R.id.refresh_btn)
        backButton = findViewById(R.id.back_btn)

        backButton.setOnClickListener {
            finish()
        }
        refreshButton.setOnClickListener { fetchWaterLevel() }
        bakImage.isClickable = false

        fetchWaterLevel()

    }

    private fun fetchWaterLevel() {
        CoroutineScope(Dispatchers.IO).launch {

            val waterResult = makeGetRequest("/water") ?: return@launch
            val waterLevel = waterResult.split("|").firstOrNull()?.toIntOrNull() ?: 0

            withContext(Dispatchers.Main) {
                    waterLevelText.text = waterLevel.toString()
                    val resId = if (waterLevel > 0) R.drawable.bak_full else R.drawable.bak_empty
                    bakImage.setImageResource(resId)
            }
        }
    }

    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("WaterActivity", "Ошибка запроса: ${e.message}")
            null
        }
    }
}