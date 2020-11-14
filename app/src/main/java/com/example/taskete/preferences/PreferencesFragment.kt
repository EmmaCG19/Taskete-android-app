package com.example.taskete.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.taskete.R


class PreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}