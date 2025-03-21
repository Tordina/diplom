package com.example.test4

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class WaterActivity : AppCompatActivity() {

    private lateinit var bakImage: ImageView
    private var isBakFull = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water)

        bakImage = findViewById(R.id.bak_image)
        bakImage.setOnClickListener { toggleBak() }

        val backButton: Button = findViewById(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun toggleBak() {
        isBakFull = !isBakFull
        bakImage.setImageResource(if (isBakFull) R.drawable.bak_full else R.drawable.bak_empty)
    }
}