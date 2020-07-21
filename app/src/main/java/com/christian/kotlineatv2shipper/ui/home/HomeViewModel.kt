package com.christian.kotlineatv2shipper.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.christian.kotlineatv2shipper.callback.IShippingOrderCallbackListener
import com.christian.kotlineatv2shipper.common.Common
import com.christian.kotlineatv2shipper.model.ShippingOrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IShippingOrderCallbackListener {
    override fun onShippingOrderLoadSuccess(shippingOrders: List<ShippingOrderModel>) {
        orderModelMutableLiveData.value = shippingOrders
    }

    override fun onShippingOrderLoadFailed(message: String) {
        messageError.value = message


    }

    private val orderModelMutableLiveData:MutableLiveData<List<ShippingOrderModel>>
    val messageError:MutableLiveData<String>
    private val listener: IShippingOrderCallbackListener
    init {
        orderModelMutableLiveData = MutableLiveData()
        messageError= MutableLiveData()
        listener=this
    }

    fun getOrderMutableLiveData(shipperPhone:String):MutableLiveData<List<ShippingOrderModel>>{
       loadOrderByShipper(shipperPhone)
       return orderModelMutableLiveData
    }

    private fun loadOrderByShipper(shipperPhone: String) {
        //get data of firebase
        val tempList:MutableList<ShippingOrderModel> = ArrayList()
        val orderRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPING_ORDER_REF)
            .orderByChild("shipperPhone")
            .equalTo(Common.currentShipperUser!!.phone)
        orderRef.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                listener.onShippingOrderLoadFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (item in p0.children){
                    val shippingOrder = item.getValue(ShippingOrderModel::class.java)
                    shippingOrder!!.key =item.key
                    tempList.add(shippingOrder!!)
                }
                listener.onShippingOrderLoadSuccess(tempList)

            }

        })


    }


}