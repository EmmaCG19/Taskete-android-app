package com.example.taskete.preferences

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.taskete.AboutMeActivity
import com.example.taskete.R


class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        launchAboutMeActivity()
    }

    private fun launchAboutMeActivity() {
        preferenceManager.findPreference<Preference>("showAboutMeSection")
            ?.setOnPreferenceClickListener(object : Preference.OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference?): Boolean {
                    Intent(activity, AboutMeActivity::class.java).apply {
                        startActivity(this)
                    }
                    return true
                }
            })
    }
}