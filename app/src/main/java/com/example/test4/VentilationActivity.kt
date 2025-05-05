package com.example.test4

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class VentilationActivity : AppCompatActivity() {
    private lateinit var btnVentOn: Button
    private lateinit var btnVentOff: Button
    private lateinit var ip: String
    private lateinit var port: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventilation)
        btnVentOn = findViewById(R.id.ventilation_btn_on)
        btnVentOff = findViewById(R.id.ventilation_btn_off)
        ip = getString(R.string.server_ip)
        port = getString(R.string.server_port)
        val backButton: Button = findViewById(R.id.back_btn)

        backButton.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.ventilation_btn_on).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    makeGetRequest("/ventilation_on")
                    withContext(Dispatchers.Main) {
                        updateButtonColors(true)
                    }
                } catch (e: Exception) {
                    Log.e("VentilationActivity", "Ошибка: ${e.message}")
                }
            }
        }

        findViewById<Button>(R.id.ventilation_btn_off).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    makeGetRequest("/ventilation_off")
                    withContext(Dispatchers.Main) {
                        updateButtonColors(false)
                    }
                } catch (e: Exception) {
                    Log.e("VentilationActivity", "Ошибка: ${e.message}")
                }
            }
        }

    }

    private fun updateButtonColors(isOn: Boolean) {
        if (isOn) {
            btnVentOn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.accentWarm)
            )
            btnVentOff.setBackgroundColor(
                ContextCompat.getColor(this, R.color.translucentDark)
            )
        } else {
            btnVentOn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.translucentDark)
            )
            btnVentOff.setBackgroundColor(
                ContextCompat.getColor(this, R.color.accentWarm)
            )
        }
    }

    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("VentilationActivity", "Ошибка запроса $endpoint: ${e.message}")
            null
        }
    }
}