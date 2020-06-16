package com.christian.kotlineatv2shipper.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.christian.kotlineatv2shipper.R
import com.christian.kotlineatv2shipper.ShippingActivity
import com.christian.kotlineatv2shipper.common.Common
import com.christian.kotlineatv2shipper.model.ShippingOrderModel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import io.paperdb.Paper
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class MyShippingOrderAdapter(var context:Context,var shippingOrderModelList:List<ShippingOrderModel>):RecyclerView.Adapter<MyShippingOrderAdapter.MyViewHolder>() {


    var simpleDateFormat:SimpleDateFormat
    init {
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        Paper.init(context)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
       return MyViewHolder(LayoutInflater.from(context!!).inflate(R.layout.layout_shipping_order,parent,false))
    }

    override fun getItemCount(): Int {
        return shippingOrderModelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context)
            .load(shippingOrderModelList.get(position).orderModel!!.cartItemList!![0].foodImage)
            .into(holder.img_food!!)
        holder.txt_date!!.text = StringBuilder(simpleDateFormat.format(shippingOrderModelList[position].orderModel!!.createDate))

        Common.setSpanStringColor("No.:",shippingOrderModelList[position].orderModel!!.key,
            holder.txt_order_number,Color.parseColor("#BA454A"))

        Common.setSpanStringColor("Address: ",shippingOrderModelList[position].orderModel!!.shippingAddress,
            holder.txt_order_address,Color.parseColor("#BA454A"))

        Common.setSpanStringColor("Metodo Pago: ",shippingOrderModelList[position].orderModel!!.transactionId,
            holder.txt_payment,Color.parseColor("#BA454A"))

        if (shippingOrderModelList[position].isStartTrip){
            holder.btn_ship_now!!.isEnabled = false
        }

        holder.btn_ship_now!!.setOnClickListener {

            Paper.book().write(Common.SHIPPING_DATA,Gson().toJson(shippingOrderModelList[0]))

            context.startActivity(Intent(context,ShippingActivity::class.java))

        }

    }


    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var img_food:ImageView?=null
        var txt_date:TextView?=null
        var txt_order_number:TextView?=null
        var txt_order_address:TextView?=null
        var txt_payment:TextView?=null
        var btn_ship_now:MaterialButton?=null

        init {
            img_food = itemView.findViewById(R.id.img_food) as ImageView
            txt_date = itemView.findViewById(R.id.txt_date) as TextView
            txt_order_number = itemView.findViewById(R.id.txt_order_number) as TextView
            txt_order_address = itemView.findViewById(R.id.txt_order_address) as TextView
            txt_payment = itemView.findViewById(R.id.txt_payment) as TextView
            btn_ship_now = itemView.findViewById(R.id.btn_ship_now) as MaterialButton
        }

    }

}