package com.christian.kotlineatv2shipper.callback

import com.christian.kotlineatv2shipper.model.ShippingOrderModel

interface IShippingOrderCallbackListener {
    fun onShippingOrderLoadSuccess(shippingOrders: List<ShippingOrderModel>)
    fun onShippingOrderLoadFailed(message: String)
}