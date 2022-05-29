package com.example.order

interface OnOrderListener {
    fun onTrack(order:Order)
    fun onStartChat(order: Order)
}