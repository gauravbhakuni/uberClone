package com.miniproject.uber

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.miniproject.uber.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val iHome = Intent(this, MainActivity::class.java)

        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(iHome)
            finish()
        }, 4000)
    }
}
