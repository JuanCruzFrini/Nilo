package com.example.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.Producto
import com.example.nilo.R
import com.example.nilo.databinding.ItemProductoCartBinding

class ProductCartAdapter(
    private val productList: MutableList<Producto>,
    private val listener:OnCartListener)
    : RecyclerView.Adapter<ProductCartAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_producto_cart, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setListener(productList[position])
        holder.binding.txtName.text = productList[position].name
        holder.binding.txtQuantity.text = productList[position].newQuantity.toString()

        Glide.with(holder.itemView.context).load(productList[position].imgUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.time_lapse)
            .error(R.drawable.broken_image)
            .centerCrop()
            .circleCrop()
            .into(holder.binding.imgProducto)
    }

    override fun getItemCount(): Int = productList.size

    //agrega producto a Firestore
    fun addProduct(producto: Producto){
        if (!productList.contains(producto)){
            productList.add(producto)
            notifyItemInserted(productList.size - 1)
            calcTotal()
        } else {
            updateProduct(producto)
        }
    }

    //actualiza producto de Firestore
    fun updateProduct(producto: Producto){
        val index = productList.indexOf(producto)
        if (index != -1){
            productList.set(index, producto)
            notifyItemChanged(index)
            calcTotal()
        }
    }

    //elimina producto de Firestore
    fun deleteProduct(producto: Producto){
        val index = productList.indexOf(producto)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
            calcTotal()
        }
    }

    private fun calcTotal(){
        var result = 0.0
        for (producto in productList){
            result += producto.totalPrice()
        }
        listener.showTotal(result)
    }

    fun getProductos() : List<Producto> = productList

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = ItemProductoCartBinding.bind(itemView)

        fun setListener(producto: Producto){
            binding.imgBtnSum.setOnClickListener {
                producto.newQuantity += 1
                //if (producto.newQuantity < producto.quantity) producto.newQuantity += 1
                listener.setQuantity(producto)
            }
            binding.imgBtnRest.setOnClickListener {
                producto.newQuantity -= 1
                /*if (producto.newQuantity >= 1) producto.newQuantity -= 1
                if (producto.newQuantity == 0){
                    deleteProduct(producto)
                }*/
                listener.setQuantity(producto)
            }
        }
    }
}