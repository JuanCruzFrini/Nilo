package com.example.order

import com.example.nilo.ProductoOrder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Order(
    @get:Exclude var id:String = "",
    var clientId:String = "",
    var products:Map<String,ProductoOrder> = hashMapOf(),
    var totalPrice:Double = 0.0,
    var status:Int = 0,
    @ServerTimestamp var date:Timestamp? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Order

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
/*
    @ServerTimestamp:
    Con esta anotacion, al insertar una nueva orden, el servidor
    va a detectar la fecha actual y se la inserta automaticamente
 */
