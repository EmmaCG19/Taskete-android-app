package com.example.taskete

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.example.taskete.data.User
import com.example.taskete.db.UsersDAO
import com.example.taskete.helpers.UIManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

private const val TAG_ACTIVITY = "LoginFormActivity"
const val LOGGED_USER = "LoggedUser"

class LoginFormActivity : AppCompatActivity() {
    private lateinit var etMail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var txtRegisterLink: TextView
    private lateinit var validUser: User
    private lateinit var registeredUsers: List<User>
    private val compositeDisposable = CompositeDisposable()

    private val usersDAO: UsersDAO by lazy {
        UsersDAO(this@LoginFormActivity.applicationContext)
    }

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
        if (inputIsValid()) {
            validateLogin()
        } else {
            showLoginErrorMessage()
        }
    }

    private fun validateUserCredentials() {
        val mail = getText(etMail)
        val pass = getText(etPassword)

        val user = registeredUsers.firstOrNull { user ->
                    user.mail.equals(mail, ignoreCase = true) &&
                    user.password.equals(pass, ignoreCase = false)
        }

        if (user != null) {
            launchUserSession(user)
        } else {
            showLoginErrorMessage()
        }

    }

    private fun launchUserSession(user: User) {
        showLoginSuccessMessage()
        Handler().postDelayed({
            Intent(this, MainActivity::class.java).apply {
                putExtra(LOGGED_USER, user)
                startActivity(this)
            }
        }, 1000)

    }

    private fun validateLogin() {
        usersDAO.getUsers().subscribe(object : SingleObserver<List<User>> {
            override fun onSubscribe(d: Disposable?) {
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: List<User>) {
                registeredUsers = t
                validateUserCredentials()
            }

            override fun onError(e: Throwable?) {
                Log.d(TAG_ACTIVITY, "Error when retrieving users because $e")
            }

        })
    }

    private fun inputIsValid(): Boolean = mailIsValid() and (passIsValid())

    private fun mailIsValid(): Boolean {
        val mail = getText(etMail)
        val validMailRegex = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

        //Not empty
        if (mail.trim().isNullOrEmpty()) {
            return false
        }

        //Valid mail Regex
        if (!mail.contains(validMailRegex)) {
            return false
        }

        return true
    }


    private fun passIsValid(): Boolean {
        val pass = getText(etPassword)

        //Not empty (both)
        if (pass.trim().isNullOrEmpty()) {
            return false
        }

        return true
    }

    private fun goToRegisterForm() {
        Intent(this, RegisterFormActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun showLoginSuccessMessage() {
        UIManager.showMessage(this, resources.getText(R.string.loginSuccess) as String)
    }

    private fun showLoginErrorMessage() {
        UIManager.showMessage(this, resources.getText(R.string.loginError) as String)
    }

    private fun getText(view: EditText): String = view.text.toString()

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}

