package com.example.test4

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {

    // lateinit чтобы не использовать .? (они точно будут не null)
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var resultInfo: TextView
    private lateinit var tempInfo: TextView
    //вместо lamp123 теперь можно просто обращаться к lampViews[0], lampViews[1], lampViews[2]
    private lateinit var lampViews: List<ImageView>
    private lateinit var lampInfos: List<TextView>

    private var lampCount = 0
    private var temperature = 0
    private val ip = "172.20.10.2"
    private val port = "8000"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlus = findViewById(R.id.btn_plus)
        btnMinus = findViewById(R.id.btn_minus)
        resultInfo = findViewById(R.id.result_info)
        tempInfo = findViewById(R.id.temp_info)

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

        btnPlus.setOnClickListener { adjustTemperature(1) }
        btnMinus.setOnClickListener { adjustTemperature(-1) }

        //вместо lamp123?.setOnClickListener { toggleLamp(123, lamp123, lamp123_info) }
        //при клике на каждый ImageView в коллекции, будет вызван метод toggleLamp
        //в качестве аргумента будет передан номер  элемента, увеличенный на 1
        lampViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { toggleLamp(index + 1) }
        }

        Timer().schedule(timerTask {
            CoroutineScope(Dispatchers.IO).launch { updateUi() }
        }, 0, 5000)

        CoroutineScope(Dispatchers.IO).launch {
            val response = makeGetRequest("/config") ?: return@launch //// Выход из текущей корутины, если выполнено условие
            lampCount = response.toIntOrNull() ?: 0
            Log.d("MainActivity", "Количество ламп: $lampCount")
            withContext(Dispatchers.Main) { updateLampUIs() }
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
//Запрос вынесен в makeGetRequest()
//Если сервер не ответил, метод просто выйдет (return@launch)
    private fun updateUi() {
        CoroutineScope(Dispatchers.IO).launch {
            val batteryResult = makeGetRequest("/bat") ?: return@launch
            val temperatureResult = makeGetRequest("/temp") ?: return@launch

            withContext(Dispatchers.Main) {
                resultInfo.text = batteryResult.split("|").firstOrNull() ?: "Нет данных"
                tempInfo.text = temperatureResult.split("|").firstOrNull() ?: "Нет данных"
                temperature = temperatureResult.split("|").firstOrNull()?.toIntOrNull() ?: temperature
            }
        }
    }
//ImageView и TextView теперь не передаются как параметры, а берутся из списка lampViews и lampInfos
//вместо if (lampImageView == null) теперь просто обращаемся к lampViews[lampId - 1]
    private fun toggleLamp(lampId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val command = if (lampViews[lampId - 1].isSelected) "off" else "on"
            val response = makeGetRequest("/lamp${lampId}_$command")
            if (response == "OK") {
                withContext(Dispatchers.Main) {
                    val isOn = !lampViews[lampId - 1].isSelected
                    lampViews[lampId - 1].isSelected = isOn
                    updateLampInfo(lampId, isOn)
                }
            }
        }
    }

    //ля каждого элемента в lampViews вызывается функция updateLampInf
    // которой передаются индекс элемента (сдвинутый на 1) и его состояние выбора
    private fun updateLampUIs() {
        lampViews.forEachIndexed { index, imageView ->
            updateLampInfo(index + 1, imageView.isSelected)
        }
    }
  // updateLampInfo теперь работает с массивом
    private fun updateLampInfo(lampId: Int, isOn: Boolean) {
        lampInfos.getOrNull(lampId - 1)?.text = "Лампа $lampId: ${if (isOn) "Включена" else "Выключена"}"
        lampViews.getOrNull(lampId - 1)?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
    }

    //Вынесены HTTP-запросы в отдельные функции
    private fun makeGetRequest(endpoint: String): String? {
        return try {
            URL("http://$ip:$port$endpoint").readText()
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка запроса $endpoint: ${e.message}")
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
            Log.e("MainActivity", "Ошибка POST запроса $endpoint: ${e.message}")
            null
        }
    }
}
