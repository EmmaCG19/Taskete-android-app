package com.example.taskete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterFormActivity : AppCompatActivity() {
    private lateinit var etUsername: TextInputEditText
    private lateinit var etMail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etConfirmPass: TextInputEditText
    private lateinit var btnRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_form)
        setupUI()
    }

    private fun setupUI() {
        etUsername = findViewById(R.id.etRegisterUsername)
        etMail = findViewById(R.id.etRegisterMail)
        etPass = findViewById(R.id.etRegisterPass)
        etConfirmPass = findViewById(R.id.etRegisterConfirmPass)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener{
            createAccount()
        }
    }

    private fun validateCredentials() {
        TODO("Not yet implemented")
    }

    private fun createAccount(){
        TODO("Not yet implemented")
    }


}