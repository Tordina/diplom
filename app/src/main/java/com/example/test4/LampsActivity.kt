package com.example.test4

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class LampsActivity : AppCompatActivity() {

    private lateinit var lampViews: List<ImageView>
    private lateinit var lampInfos: List<TextView>
    private lateinit var lampButtons: List<Button>
    private lateinit var progressBar: ProgressBar
    private var lampCount = 0
    private lateinit var ip: String
    private lateinit var port: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lamps)
        ip = getString(R.string.server_ip)
        port = getString(R.string.server_port)
        lampViews = listOf(
            findViewById(R.id.lamp1),
            findViewById(R.id.lamp2),
            findViewById(R.id.lamp3)
        )
        lampInfos = listOf(
            findViewById(R.id.lamp1_info),
            findViewById(R.id.lamp2_info),
            findViewById(R.id.lamp3_info)
        )
        lampButtons = listOf(
            findViewById(R.id.button1),
            findViewById(R.id.button2),
            findViewById(R.id.button3)
        )
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.VISIBLE

        initializeLampCount()

        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        lampButtons.forEachIndexed { index, switch ->
            switch.setOnClickListener() {
                toggleLamp(index + 1)
            }
        }

        Timer().schedule(timerTask {
            updateLampStates()
        }, 0, 1000)
    }

    override fun onResume() {
        super.onResume()
        updateLampStates()
    }

    private fun updateLampStates() {
        CoroutineScope(Dispatchers.IO).launch {

            for (i in 1..lampCount) {
                val response = makeGetRequest("/lamp${i}_state") ?: continue
                val isOn = response.trim() == "ON"

                withContext(Dispatchers.Main) {
                    lampViews[i - 1].isSelected = isOn
                    lampInfos.getOrNull(i - 1)?.text =
                        "Лампа $i: ${if (isOn) "Включена" else "Выключена"}"
                    lampViews.getOrNull(i - 1)
                        ?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
                    lampButtons.getOrNull(i - 1)?.text = if (isOn) "Вкл" else "Выкл"
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun initializeLampCount() {
        CoroutineScope(Dispatchers.IO).launch {

            val response = makeGetRequest("/config") ?: return@launch
            lampCount = response.toIntOrNull() ?: 0

            if (lampCount == 0) {
                Log.e("LampsActivity", "Ошибка: количество ламп равно 0")
                withContext(Dispatchers.Main) {
                    lampInfos[0].text = "Ошибка: не удалось загрузить количество ламп"
                    progressBar.visibility = View.GONE
                }
                return@launch
            }
        }
    }

    private fun toggleLamp(lampId: Int) {
        CoroutineScope(Dispatchers.IO).launch {

            val isCurrentlyOn = lampViews[lampId - 1].isSelected
            val command = if (lampViews[lampId - 1].isSelected) "off" else "on"

            withContext(Dispatchers.Main) {
                lampButtons[lampId - 1].text = if (!isCurrentlyOn) "Вкл" else "Выкл"
                lampViews[lampId - 1].setImageResource(R.drawable.loading)
            }

            makeGetRequest("/lamp${lampId}_$command")
        }
    }

    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("LampsActivity", "Ошибка запроса $endpoint: ${e.message}")
            null
        }
    }
}