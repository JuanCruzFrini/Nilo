package com.example.nilo

interface MainAux {
    fun getProductsCart(): MutableList<Producto>
    fun updateTotal()
    fun clearCart()
    fun getProductoSelected(): Producto?

    fun showButton(isVisible:Boolean)
    fun addProductoToCart(producto: Producto)
}