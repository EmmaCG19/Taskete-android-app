package com.example.taskete.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.taskete.R


class AboutMeActivity : AppCompatActivity() {
    private lateinit var btnLinkRepo: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_me)
        setupUI()
    }

    private fun setupUI() {
        btnLinkRepo = findViewById(R.id.iconRepo)
        btnLinkRepo.setOnClickListener {
            val uri = Uri.parse("http://github.com/EmmaCG19")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }
}