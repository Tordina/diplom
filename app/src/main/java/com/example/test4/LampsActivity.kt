package com.example.test4

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class LampsActivity : AppCompatActivity() {

    private lateinit var lampViews: List<ImageView>
    private lateinit var lampInfos: List<TextView>
    private lateinit var lampSwitches: List<SwitchMaterial>
    private var lampCount = 0

    private val ip = "192.168.0.103"
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

        initializeLampCount()
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
        
    }

    private fun initializeLampCount() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = makeGetRequest("/config") ?: return@launch
            lampCount = response.toIntOrNull() ?: 0
            Log.d("LampsActivity", "Количество ламп: $lampCount")
            withContext(Dispatchers.Main) { updateLampUIs() }
        }
    }

    private fun toggleLamp(lampId: Int, switchState: Boolean? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val isCurrentlyOn = lampViews[lampId - 1].isSelected
            val shouldTurnOn = switchState ?: !isCurrentlyOn
            val command = if (shouldTurnOn) "on" else "off"

            val response = makeGetRequest("/lamp${lampId}_$command")
            if (response == "OK") {
                withContext(Dispatchers.Main) {
                    lampViews[lampId - 1].isSelected = shouldTurnOn
                    lampSwitches[lampId - 1].isChecked = shouldTurnOn
                    updateLampInfo(lampId, shouldTurnOn)
                }
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