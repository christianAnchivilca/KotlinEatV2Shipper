package com.christian.kotlineatv2shipper.model

class TokenModel {

    var uid:String?=null
    var token:String?=null
    var serverToken:Boolean = false
    var shipperToken:Boolean =false


    constructor(){}
    constructor(uid: String?, token: String?,serverToken: Boolean,shipperToken: Boolean) {
        this.uid = uid
        this.token = token
        this.shipperToken = shipperToken
        this.serverToken = serverToken
    }


}