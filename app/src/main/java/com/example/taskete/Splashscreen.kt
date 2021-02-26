package com.example.taskete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.taskete.preferences.SessionManager

class Splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        //Fetch logged user info
        //1- If user is null -> launchLoginActivity
        //2- If user != null -> launchMainActivity
        callSessionManager()

        Handler(mainLooper).postDelayed(
                {
                    if (SessionManager.restoreLoggedUser() != SessionManager.DEFAULT_USER_ID) {
                        launchMainActivity()
                    } else {
                        launchLoginActivity()
                    }

                    finish()
                }, 1500
        )

    }

    private fun callSessionManager() {
        SessionManager.getPreferences(this@Splashscreen.applicationContext)
    }

    private fun launchMainActivity() {
        Intent(this, MainActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun launchLoginActivity() {
        Intent(this, LoginFormActivity::class.java).apply {
            startActivity(this)
        }
    }
}