package com.example.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.nilo.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val switchPreferenceCompat = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_offers_key))
        switchPreferenceCompat?.setOnPreferenceChangeListener { preference, newValue ->
            (newValue as? Boolean)?.let { isChecked->
                //topicos para la suscripcion de notis. masivas
                val topic = getString(R.string.settings_topic_offers)
                if (isChecked){
                    //nos suscribimos
                    Firebase.messaging.subscribeToTopic(topic)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    //nos desuscribimos
                    Firebase.messaging.unsubscribeFromTopic(topic)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            true
        }
    }
}