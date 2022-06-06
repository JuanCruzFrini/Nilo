package com.example.order

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.message.ChatFragment
import com.example.nilo.Constants
import com.example.nilo.ProductAdapter
import com.example.nilo.R
import com.example.nilo.databinding.ActivityOrderBinding
import com.example.track.OrderAux
import com.example.track.TrackFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux{

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected:Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setRecyclerView()
        setupFirestore()
    }

    private fun setRecyclerView() {
       adapter = OrderAdapter(mutableListOf(), this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    override fun onTrack(order: Order) {
        orderSelected = order
        val fragment = TrackFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStartChat(order: Order) {
        orderSelected = order
        val fragment = ChatFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun getOrderSelected(): Order = orderSelected

    private fun setupFirestore(){
        FirebaseAuth.getInstance().currentUser?.let { user->
            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_REQUESTS)
                //Para combinar (p.ej).orderBy() + .whereEqualTo(), hay que crear un "Index" en Firebase
                .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
                //Para que se carguen los datos de ese user y no de otro/s
                .whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                //Filtros
                /*.whereIn(Constants.PROP_STATUS, listOf(1,2))
                .whereNotIn(Constants.PROP_STATUS, listOf(4))
                .whereGreaterThan(Constants.PROP_STATUS, 2)
                .whereLessThan(Constants.PROP_STATUS, 4)
                .whereEqualTo(Constants.PROP_STATUS, 3)
                .whereGreaterThanOrEqualTo(Constants.PROP_STATUS, 2)*/
                //Esto es un indice
                /*.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                .orderBy(Constants.PROP_STATUS, Query.Direction.DESCENDING)*/
                //Esto es un indice
                /*.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                .orderBy(Constants.PROP_STATUS, Query.Direction.ASCENDING)
                .whereLessThan(Constants.PROP_STATUS, 4)
                .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)*/
                .get()
                .addOnSuccessListener {
                    for (document in it){
                        val order = document.toObject(Order::class.java)
                        order.id = document.id
                        adapter.addOrder(order)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
                }
        }
    }
}