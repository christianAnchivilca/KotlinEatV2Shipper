package com.christian.kotlineatv2shipper.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.christian.kotlineatv2shipper.R
import com.christian.kotlineatv2shipper.model.ShipperUserModel
import com.christian.kotlineatv2shipper.model.TokenModel
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random


object Common {

    val SHIPPING_DATA: String?="ShippingData"
    val SHIPPING_ORDER_REF: String = "ShippingOrder"
    val ORDER_REF:String ="Order"

    const val SHIPPER_REF = "Shippers"
    var currentShipperUser: ShipperUserModel?=null
    const val CATEGORY_REFERENCE = "Category"
    val DEFAULT_COLUMN_COUNT:Int = 0
    val FULL_WIDTH_COLUMN:Int = 1
    const val TOKEN_REF: String = "Tokens"
    const val NOTI_CONTENT: String = "content"
    const val NOTI_TITLE: String = "title"

    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {

        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder,TextView.BufferType.SPANNABLE)
    }

    fun setSpanStringColor(welcome: String, name: String?, txtUser: TextView?,color:Int){
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        txtSpannable.setSpan(ForegroundColorSpan(color),0,name!!.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder,TextView.BufferType.SPANNABLE)

    }

    fun getDateOfWeek(get: Int): String {
        when(get){
            1 -> return "Lunes"
            2 -> return "Martes"
            3 -> return "Miercoles"
            4 -> return "Jueves"
            5 -> return "Viernes"
            6 -> return "Sabado"
            7 -> return "Domingo"
            else -> return "Unknow"
        }
    }

    fun createOrderNumber(): String {
        return StringBuilder()
            .append(System.currentTimeMillis())
            .append(Math.abs(Random.nextInt()))
            .toString()

    }

    fun convertStatusToText(orderStatus: Int): String {
        when(orderStatus){

            0->return "Placed"
            1->return "Shipping"
            2->return "Shipped"
            -1->return "Cancelled"
            else -> return "Unknow"

        }

    }

    fun convertStatusToString(orderStatus: Int): String? =
        when(orderStatus){
            0 -> "Placed"
            1 -> "Shipping"
            2-> "Shipped"
            -1 -> "Canceled"
            else -> "Error"
        }

    fun updateToken(context: Context, token: String,isServerToken:Boolean,isShipperToken:Boolean) {
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(Common.currentShipperUser!!.uid!!)
            .setValue(TokenModel(currentShipperUser!!.phone!!,token,isServerToken,isShipperToken))
            .addOnFailureListener{
                    e->
                Toast.makeText(context,""+e.message,Toast.LENGTH_LONG).show()
            }

    }

    fun showNotification(context:Context, id: Int, title: String?, content: String?,intent: Intent?) {
        var pendingIntent: PendingIntent? = null
        if (intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "edmt.dev.eatitv2"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"Eat It v2",
                NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.description="Eat It v2"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title!!).setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)

            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_restaurant_menu_black_24dp))
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)
        val notification = builder.build()
        notificationManager.notify(id,notification)


    }

    fun getNewOrderTopic(): String {
        return StringBuilder("/topics/new_order").toString()

    }


}