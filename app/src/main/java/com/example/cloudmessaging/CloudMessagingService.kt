package com.example.cloudmessaging

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.nilo.Constants
import com.google.firebase.messaging.FirebaseMessagingService

class CloudMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        registerNewTokenLocal(token)
    }

    private fun registerNewTokenLocal(newToken:String){
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        preferences.edit {
            putString(Constants.PROP_TOKEN, newToken).apply()
        }

        Log.i("new token", newToken)
    }
}
//Con este servicio, y mediante la consola de Firebase, enviamos
//una notificacion push por medio del token a un dispositivo en especifico
