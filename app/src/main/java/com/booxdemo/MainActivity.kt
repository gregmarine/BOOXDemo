package com.booxdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.booxdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.btnClear.setOnClickListener { binding.drawingView.clearCanvas() }
    }

    override fun onResume() {
        super.onResume()
        binding.drawingView.enable()
    }

    override fun onPause() {
        super.onPause()
        binding.drawingView.disable()
    }
}
