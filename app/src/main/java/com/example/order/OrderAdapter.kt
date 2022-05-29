package com.example.order

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nilo.R
import com.example.nilo.databinding.ItemOrderBinding

class OrderAdapter(
    val orderList:MutableList<Order>,
    val listener: OnOrderListener)
    : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private lateinit var context: Context

    private val arrayValues:Array<String> by lazy {
        context.resources.getStringArray(R.array.status_values)
    }
    private val arrayKeys:Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderAdapter.ViewHolder {
        context = parent.context
        return  ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_order, parent, false))
    }


    override fun onBindViewHolder(holder: OrderAdapter.ViewHolder, position: Int) {
        val order = orderList[position]
        holder.setListener(order)

        holder.binding.txtId.text = context.getString(R.string.order_id, order.id)

        var names = ""
        order.products.forEach {
            names += "${it.value.name}"
        }
        holder.binding.txtProductNames.text = names.dropLast(2)

        holder.binding.txtTotalPrice.text = context.getString(R.string.producto_full_cart, order.totalPrice)

        val index = arrayKeys.indexOf(order.status)
        val statusStr = if (index != -1) arrayValues[index] else context.getString(R.string.order_status_unknow)
        holder.binding.txtStatus.text = context.getString(R.string.order_status, statusStr)
    }

    override fun getItemCount(): Int = orderList.size

    fun addOrder(order: Order){
        orderList.add(order)
        notifyItemInserted(orderList.size - 1)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = ItemOrderBinding.bind(itemView)

        fun setListener(order:Order){
            binding.btnTrack.setOnClickListener {
                listener.onTrack(order)
            }
            binding.chipChat.setOnClickListener {
                listener.onStartChat(order)
            }
        }
    }

}