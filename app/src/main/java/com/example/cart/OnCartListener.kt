package com.example.cart

import com.example.nilo.Producto

interface OnCartListener {
    fun setQuantity(producto: Producto)
    fun showTotal(total:Double)
}