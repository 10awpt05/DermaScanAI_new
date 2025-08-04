package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityLoadingPageBinding
import pl.droidsonroids.gif.GifDrawable

class LoadingPage : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadingPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gifDrawable = binding.logo.drawable as GifDrawable
        gifDrawable.loopCount = 1
        gifDrawable.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }, 5000)

    }
}