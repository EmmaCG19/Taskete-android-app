package com.example.taskete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFormActivity : AppCompatActivity() {
    private lateinit var etMail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var txtRegisterLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_form)
        setupUI()
    }

    private fun setupUI() {
        etMail = findViewById(R.id.etLoginMail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtRegisterLink = findViewById(R.id.txtRegisterLink)

        btnLogin.setOnClickListener {
            loginUser()
        }

        txtRegisterLink.setOnClickListener {
            goToRegisterForm()
        }

    }

    private fun loginUser() {
        TODO("Not yet implemented")
    }

    //Validar las credenciales y generar un "token" (Usuario)
    //Pasarle a la MainActivity el token
    private fun validateCredentials() {
    }

    //Ir al register form para registrar
    private fun goToRegisterForm() {
        TODO("Not yet implemented")
    }
}