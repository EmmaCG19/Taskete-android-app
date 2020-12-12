package com.example.taskete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class Splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        Handler().postDelayed(
                {
                    launchLoginActivity()
                    finish()
                }, 1500
        )

    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun launchLoginActivity() {
        val intent = Intent(this, LoginFormActivity::class.java)
        startActivity(intent)
    }
}