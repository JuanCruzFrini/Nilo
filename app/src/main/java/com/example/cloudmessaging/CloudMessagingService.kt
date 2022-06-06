package com.example.cloudmessaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.nilo.Constants
import com.example.nilo.MainActivity
import com.example.nilo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.notification?.let {
            //para mostrar img en la notif
            val imgUrl = it.imageUrl//"https://talently.tech/blog/wp-content/uploads/2020/12/bolsa-de-trabajo-de-amazon-para-programadores.jpg"
            if (imgUrl == null){
                sendNotification(it)
            } else {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(imgUrl)
                    .into(object  : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            sendNotification(it, resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    //Crea una notificacion en primer plano
    private fun sendNotification(notificacion:RemoteMessage.Notification, bitmap: Bitmap? = null){
        //indicamos que activity queremos abrir en el onclick de la notif
        val intent = Intent(this, MainActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(notificacion.title)
            .setContentText(notificacion.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setColor(ContextCompat.getColor(this, R.color.teal_900))
            .setContentIntent(pendingIntent)
            //si no tiene imagen, le da prioridad al texto
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificacion.body))

        //si la notificacion tiene imagen, se adapta para ser expandible
        if (bitmap != null){
           notificationBuilder
               .setLargeIcon(bitmap) //para mostrar miniatura en la not. contraida
               .setStyle(NotificationCompat.BigPictureStyle() //para mostrar imagen
                    .bigPicture(bitmap)
                    .bigLargeIcon(null))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //Desde Android 8 hay que configurar el canal obligatoriamente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
//Con este servicio, y mediante la consola de Firebase, enviamos
//una notificacion push por medio del token a un dispositivo en especifico
