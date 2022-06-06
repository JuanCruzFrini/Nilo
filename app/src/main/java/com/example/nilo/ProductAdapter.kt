package com.example.nilo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.databinding.ItemProductoBinding

class ProductAdapter(
    private val productList: MutableList<Producto>,
    val listener: OnProductListener)
    : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAdapter.ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false))

    override fun onBindViewHolder(holder: ProductAdapter.ViewHolder, position: Int) {
        val producto = productList[position]

        holder.setListener(producto)
        if (producto.id == null) {
            if (producto.id == null && productList.indexOf(producto) == productList.size - 1){
                holder.binding.btnMore.hide()
            }
            holder.binding.btnMore.show()
            holder.binding.containerProducto.hide()
        }
        else {
            holder.binding.btnMore.hide()
            holder.binding.containerProducto.show()
        }

        holder.binding.txtName.text = producto.name
        holder.binding.txtPrecio.text = producto.price.toString()
        holder.binding.txtQuantity.text = producto.quantity.toString()

        Glide.with(holder.itemView.context)
            .load(producto.imgUrl)
            .placeholder(R.drawable.time_lapse)
            .error(R.drawable.broken_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop().into(holder.binding.imgProducto)
    }

    override fun getItemCount(): Int = productList.size

    //agrega producto a Firestore
    fun addProduct(producto: Producto) {
        if (!productList.contains(producto)) {
            //productList.add(producto)
            productList.add(productList.size - 1, producto)
            notifyItemInserted(productList.size - 2)
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
        }
    }

    //elimina producto de Firestore
    fun deleteProduct(producto: Producto){
        val index = productList.indexOf(producto)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = ItemProductoBinding.bind(itemView)

        fun setListener(producto: Producto){
            binding.root.setOnClickListener {
                listener.onClick(producto)
            }
            binding.btnMore.setOnClickListener {
                listener.loadMore()
            }
        }
    }
}