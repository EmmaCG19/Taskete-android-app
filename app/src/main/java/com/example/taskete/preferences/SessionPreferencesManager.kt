package com.example.taskete.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.taskete.data.User
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

const val SESSION_PREFERENCES = "SessionPreferences"
const val LOGGED_USER_ID = "LoggedUserId"

class SessionPreferencesManager(private val context: Context) { //Utilizar applicationContext
    private lateinit var sharedPreferences: SharedPreferences
    val DEFAULT_USER_ID = -1


    init {
        getPreferences(context).subscribe(object : SingleObserver<SharedPreferences> {
            override fun onSubscribe(d: Disposable?) {
                Log.d(SessionPreferencesManager::class.java.name, "Waiting for shared preferences")
            }

            override fun onSuccess(t: SharedPreferences) {
                sharedPreferences = t
            }

            override fun onError(e: Throwable?) {
                Log.d(SessionPreferencesManager::class.java.name, "Couldn't retrieve Shared Preferences because $e")
            }
        })
    }


    private fun getPreferences(context: Context): Single<SharedPreferences> {
        return Single.fromCallable {
            context.getSharedPreferences(SESSION_PREFERENCES, Context.MODE_PRIVATE)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    }

    fun getLoggedUserId(): Int {
        Log.d(SessionPreferencesManager::class.java.name, "We need to get the logged userId from the Shared Preferences")
        return sharedPreferences.getInt(LOGGED_USER_ID, DEFAULT_USER_ID)
    }

    fun saveLoggedUserId(user: User?) {
        Log.d(SessionPreferencesManager::class.java.name, "We need to update the logged userId from the Shared Preferences")

        sharedPreferences.edit {
            putInt(LOGGED_USER_ID, user?.id ?: DEFAULT_USER_ID)
            commit()
        }
    }
}

