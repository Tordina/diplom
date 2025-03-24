package com.example.test4

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class LampsActivity : AppCompatActivity() {

    private lateinit var lampViews: List<ImageView>
    private lateinit var lampInfos: List<TextView>
    private lateinit var lampSwitches: List<SwitchMaterial>
    private lateinit var progressBar: ProgressBar
    private var lampCount = 0

    private val ip = "172.20.10.4"
    private val port = "8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lamps)

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
        lampSwitches = listOf(
            findViewById(R.id.switch1),
            findViewById(R.id.switch2),
            findViewById(R.id.switch3)
        )
        progressBar = findViewById(R.id.progressBar)

        //  ProgressBar при запуске
        progressBar.visibility = View.VISIBLE

        initializeLampCount()
        updateLampStates()

        lampViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { toggleLamp(index + 1) }
        }

        lampSwitches.forEachIndexed { index, switch ->
            switch.setOnCheckedChangeListener { _, isChecked ->
                toggleLamp(index + 1, isChecked)
            }
        }

        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        Timer().schedule(timerTask {
            updateLampStates()
        }, 0, 3000)
    }

    override fun onResume() {
        super.onResume()
        updateLampStates()
    }

    private fun updateLampStates() {
        CoroutineScope(Dispatchers.IO).launch {

            delay(2000) //для прогресс бара

            for (i in 1..lampCount) {
                val response = makeGetRequest("/lamp${i}_state") ?: continue
                val isOn = response.trim() == "ON"
                withContext(Dispatchers.Main) {
                    lampViews[i - 1].isSelected = isOn
                    lampSwitches[i - 1].isChecked = isOn
                    updateLampUIs()
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
            Log.d("LampsActivity", "Ответ от сервера (количество ламп): $response")

            lampCount = response.toIntOrNull() ?: 0
            Log.d("LampsActivity", "Количество ламп: $lampCount")

            if (lampCount == 0) {
                Log.e("LampsActivity", "Ошибка: количество ламп равно 0")
                withContext(Dispatchers.Main) {
                    lampInfos[0].text = "Ошибка: не удалось загрузить количество ламп"
                    progressBar.visibility = View.GONE
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                updateLampStates() // только после получения данных
            }
        }
    }

    private fun toggleLamp(lampId: Int, switchState: Boolean? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val isCurrentlyOn = lampViews[lampId - 1].isSelected
            val shouldTurnOn = switchState ?: !isCurrentlyOn
            val command = if (shouldTurnOn) "on" else "off"

            withContext(Dispatchers.Main) {
                lampSwitches[lampId - 1].isEnabled = false
                lampInfos[lampId - 1].text = "Лампа $lampId: обновление..."
                lampViews[lampId - 1].setImageResource(R.drawable.loading)
            }

            val response = makeGetRequest("/lamp${lampId}_$command")

            withContext(Dispatchers.Main) {
                if (response == "OK") {
                    lampViews[lampId - 1].isSelected = shouldTurnOn
                    lampSwitches[lampId - 1].isChecked = shouldTurnOn
                    updateLampInfo(lampId, shouldTurnOn)
                } else {
                    updateLampInfo(lampId, isCurrentlyOn)
                }
                lampSwitches[lampId - 1].isEnabled = true
            }
        }
    }

    private fun updateLampUIs() {
        lampViews.forEachIndexed { index, imageView ->
            updateLampInfo(index + 1, imageView.isSelected)
        }
    }

    private fun updateLampInfo(lampId: Int, isOn: Boolean) {
        lampInfos.getOrNull(lampId - 1)?.text = "Лампа $lampId: ${if (isOn) "Включена" else "Выключена"}"
        lampViews.getOrNull(lampId - 1)?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
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