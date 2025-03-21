package com.example.test4

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.but_btn).setOnClickListener {
            startActivity(Intent(this, BatteryActivity::class.java))
        }
        findViewById<Button>(R.id.lamps_btn).setOnClickListener {
            startActivity(Intent(this, LampsActivity::class.java))
        }
        findViewById<Button>(R.id.temp_btn).setOnClickListener {
            startActivity(Intent(this, TemperatureActivity::class.java))
        }
        findViewById<Button>(R.id.water_btn).setOnClickListener {
            startActivity(Intent(this, WaterActivity::class.java))
        }
    }
}