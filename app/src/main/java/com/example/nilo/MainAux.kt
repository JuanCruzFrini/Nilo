package com.example.nilo

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase

interface MainAux {
    fun getProductsCart(): MutableList<Producto>
    fun updateTotal()
    fun clearCart()
    fun getProductoSelected(): Producto?

    fun showButton(isVisible:Boolean)
    fun addProductoToCart(producto: Producto)

    fun updateTitle(user: FirebaseUser)
}