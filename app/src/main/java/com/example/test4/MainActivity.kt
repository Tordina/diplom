package com.example.test4

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.URL
import java.util.Timer
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {
    private var main_btn: Button? = null
    private var result_info: TextView? = null
    private var lamp1: ImageView? = null
    private var lamp2: ImageView? = null
    private var lamp3: ImageView? = null
    private var lamp1_info: TextView? = null
    private var lamp2_info: TextView? = null
    private var lamp3_info: TextView? = null
    private var lampCount: Int = 0
    private var ip: String = "192.168.88.21"
    private var port: String = "8000"
    private var job: Job? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_btn = findViewById(R.id.main_btn)
        result_info = findViewById(R.id.result_info)
        lamp1 = findViewById(R.id.lamp1)
        lamp2 = findViewById(R.id.lamp2)
        lamp3 = findViewById(R.id.lamp3)
        lamp1_info = findViewById(R.id.lamp1_info)
        lamp2_info = findViewById(R.id.lamp2_info)
        lamp3_info = findViewById(R.id.lamp3_info)

        Timer().schedule( timerTask {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = URL("http://$ip:$port/bat").readText()
                    Log.d("MainActivity", "Батарея: $result")
                    withContext(Dispatchers.Main) {
                        result_info?.text = result
                    }
                } catch (ex: Exception) {
                    Log.e("MainActivity", "Ошибка получения данных: ${ex.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка получения данных",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }, 0L, 5000L)

        /*main_btn?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = URL("http://$ip:$port/bat").readText()
                    withContext(Dispatchers.Main) {
                        result_info?.text = result
                    }
                } catch (ex: Exception) {
                    Log.e("MainActivity", "Ошибка получения данных: ${ex.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка получения данных",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }*/

        // Запрос конфигурации
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL("http://$ip:$port/config").readText()
                lampCount = response.toIntOrNull() ?: 0
                Log.d("MainActivity", "Количество ламп: $lampCount")
                withContext(Dispatchers.Main) {
                    updateLampUIs()
                }
            } catch (ex: Exception) {
                Log.e("MainActivity", "Ошибка получения конфигурации: ${ex.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка получения конфигурации",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Обработчики кликов
        lamp1?.setOnClickListener { toggleLamp(1, lamp1, lamp1_info) }
        lamp2?.setOnClickListener { toggleLamp(2, lamp2, lamp2_info) }
        lamp3?.setOnClickListener { toggleLamp(3, lamp3, lamp3_info) }
    }


    @SuppressLint("SuspiciousIndentation")
    private fun toggleLamp(lampId: Int, lampImageView: ImageView?, lampInfo: TextView?) {
        if (lampImageView == null) {
            Log.e("MainActivity", "ImageView is null for lamp $lampId")
            Toast.makeText(this@MainActivity, "Ошибка: не удалось найти ImageView", Toast.LENGTH_SHORT).show()
            return // Выходим из функции, если ImageView null
        }

        val command = if (lampImageView.isSelected) "off" else "on"

            Log.d("MainActivity", "Отправка запроса: /lamp${lampId}_$command")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = URL("http://$ip:$port/lamp${lampId}_$command").readText()
                    Log.d("MainActivity", "Получен ответ: $result")
                    withContext(Dispatchers.Main) {
                        if (result == "OK") {
                            lampImageView.isSelected = !lampImageView.isSelected
                            updateLampInfo(lampId, lampImageView.isSelected, lampInfo)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка управления лампой: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("MainActivity", "Ошибка управления лампой: ${ex.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка управления лампой",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

    }

    private fun updateLampUIs() {

        updateLampInfo(1, lamp1?.isSelected == true, lamp1_info)
        updateLampInfo(2, lamp2?.isSelected == true, lamp2_info)
        updateLampInfo(3, lamp3?.isSelected == true, lamp3_info)
    }

    private fun updateLampInfo(lampId: Int, isOn: Boolean, lampInfo: TextView?) {
        lampInfo?.text = "Лампа $lampId: ${if (isOn) "Включена" else "Выключена"}"
        when (lampId) {
            1 -> lamp1?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
            2 -> lamp2?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
            3 -> lamp3?.setImageResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
        }
    }
}




//package com.example.test4
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Switch
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import kotlinx.coroutines.*
//import java.net.URL
//import java.net.HttpURLConnection
//
//class MainActivity : AppCompatActivity() {
//
//    private var user_field: EditText? = null
//    private var main_btn: Button? = null
//    private var result_info: TextView? = null
//    private var name_switch: Switch? = null
//    private var name_switch2: Switch? = null
//    private var name_switch3: Switch? = null
//    private var lamp_info: TextView? = null
//    private var lamp_info2: TextView? = null
//    private var lamp_info3: TextView? = null
//    private var lampCount: Int = 0
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        user_field = findViewById(R.id.user_field)
//        main_btn = findViewById(R.id.main_btn)
//        result_info = findViewById(R.id.result_info)
//        name_switch = findViewById(R.id.name_switch)
//        name_switch2 = findViewById(R.id.name_switch2)
//        name_switch3 = findViewById(R.id.name_switch3)
//        lamp_info = findViewById(R.id.lamp_info)
//        lamp_info2 = findViewById(R.id.lamp_info2)
//        lamp_info3 = findViewById(R.id.lamp_info3)
//
//        // Запрос количества ламп на сервер
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = URL("http://172.20.10.3:8000/config").readText()
//                lampCount = response.toInt()
//                Log.d("MainActivity", "Количество ламп: $lampCount")
//                // Обновляем UI после получения данных
//                withContext(Dispatchers.Main) {
//                    updateLampUIs()
//                }
//            } catch (ex: Exception) {
//                Log.e("MainActivity", "Ошибка получения конфигурации: ${ex.message}")
//                // Обработка ошибки - показать сообщение пользователю
//                Toast.makeText(this@MainActivity, "Ошибка получения конфигурации", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        main_btn?.setOnClickListener {
//            CoroutineScope(Dispatchers.IO).launch {
//                try {          val result = URL("http://172.20.10.3:8000/bat").readText()
//                    withContext(Dispatchers.Main) {
//                        result_info?.text = result
//                    }
//                } catch (ex: Exception) {
//                    Log.e("MainActivity", "Ошибка получения данных: ${ex.message}")
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MainActivity, "Ошибка получения данных", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//
//        // Обработчики для переключателей
//        name_switch?.setOnClickListener { toggleLamp(1, name_switch, lamp_info) }
//        name_switch2?.setOnClickListener { toggleLamp(2, name_switch2, lamp_info2) }
//        name_switch3?.setOnClickListener { toggleLamp(3, name_switch3, lamp_info3) }
//    }
//
//    private fun updateLampUIs() {
//
//        // Начальное состояние переключателей
//        name_switch?.isChecked = false
//        name_switch2?.isChecked = false
//        name_switch3?.isChecked = false
//
//        // Обновляем текст состояния ламп
//        lamp_info?.text = "Лампочка выключена"
//        lamp_info2?.text = "Лампочка выключена"
//        lamp_info3?.text = "Лампочка выключена"
//    }
//
//    private fun toggleLamp(lampId: Int, switch: Switch?, lampInfo: TextView?) {
//        val command = if (switch?.isChecked == true) "on" else "off"
//
//        if (command == "on") {
//            lampInfo?.text = "Лампочка включена"
//            Toast.makeText(this@MainActivity, "Лампочка включена", Toast.LENGTH_SHORT).show()
//        } else {
//            lampInfo?.text = "Лампочка выключена"
//            Toast.makeText(this@MainActivity, "Лампочка выключена", Toast.LENGTH_SHORT).show()
//        }
//
//        CoroutineScope(Dispatchers.IO).launch {
//            //try {
//            try {
//                val result = URL("http://172.20.10.3:8000/lamp${lampId}_$command").readText()
//                withContext(Dispatchers.Main) {
//                    //result_info?.text = result
//                }
//            } catch (ex: Exception) {
//                Log.e("MainActivity", "Ошибка получения данных: ${ex.message}")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MainActivity, "Ошибка получения данных", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//        }
//    }
//}
//
//
//
//


/*val url = URL("http://172.20.10.3:8000/lamp${lampId}_$command")
   val connection = url.openConnection() as HttpURLConnection
   // Выполняем запрос
   connection.requestMethod = "GET"
   connection.connect()
   */


// Обработка ответа сервера
//val responseCode = connection.responseCode
//Log.d("MainActivity", "Ответ сервера: $responseCode")


//if (responseCode == 200) {

/*withContext(Dispatchers.Main) {
    if (command == "on") {
        //lampInfo?.text = "Лампочка включена"
        Toast.makeText(this@MainActivity, "Лампочка включена", Toast.LENGTH_SHORT).show()
    } else {
        //lampInfo?.text = "Лампочка выключена"
        Toast.makeText(this@MainActivity, "Лампочка выключена", Toast.LENGTH_SHORT).show()
    }
}*/
//} else {
//    withContext(Dispatchers.Main) {
//        Toast.makeText(this@MainActivity, "Ошибка управления лампой", Toast.LENGTH_SHORT).show()
//    }
//}

/*} catch (ex: Exception) {
    Log.e("MainActivity", "Ошибка управления лампой: ${ex.message}")
    withContext(Dispatchers.Main) {
        Toast.makeText(this@MainActivity, "Ошибка управления лампой", Toast.LENGTH_SHORT).show()
    }
}*/