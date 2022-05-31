package com.example.cart

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nilo.*
import com.example.nilo.databinding.ActivityMainBinding
import com.example.nilo.databinding.FragmentCartBinding
import com.example.order.Order
import com.example.order.OrderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class CartFragment : BottomSheetDialogFragment(), OnCartListener {

    private var binding: FragmentCartBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private lateinit var adapter:ProductCartAdapter
    private var totalPrice = 0.0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentCartBinding.inflate(LayoutInflater.from(activity))
        binding?.let {
            val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
            bottomSheetDialog.setContentView(it.root)

            bottomSheetBehavior = BottomSheetBehavior.from(it.root.parent as View)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            setRecyclerView()
            setUpButtons()
            getProductos()
            configAnalytics()
            return bottomSheetDialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun setRecyclerView() {
        binding?.let {
            adapter = ProductCartAdapter(mutableListOf(), this)

            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@CartFragment.adapter
            }

           /* (0..5).forEach {
                val producto = Producto(
                    id = it.toString(),
                    name = "Producto $it",
                    description = "This product is $it",
                    imgUrl = "",
                    quantity = it,
                    price = 2 * it.toDouble()
                )
                adapter.addProduct(producto)
            }*/
        }
    }

    override fun setQuantity(producto: Producto) {
        adapter.updateProduct(producto)
    }

    override fun showTotal(total: Double) {
        totalPrice = total
        binding?.let {
            it.txtTotal.text = getString(R.string.producto_full_cart, total)
        }
    }

    private fun setUpButtons(){
        binding?.let {
            it.imgBtnCancel.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            it.extendFab.setOnClickListener {
                requestOrder()
            }
        }
    }

    private fun requestOrder(){
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->
            enableUi(false)
            val productos = hashMapOf<String, ProductoOrder>()
            adapter.getProductos().forEach {
                productos.put(it.id!!, ProductoOrder(it.id!!, it.name!!,it.newQuantity))
            }

            val order = Order(clientId = myUser.uid, products = productos, totalPrice = totalPrice, status = 1)

            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_REQUESTS)
                .add(order)
                .addOnSuccessListener {
                    dismiss()
                    (activity as? MainAux)?.clearCart()
                    startActivity(Intent(context, OrderActivity::class.java))
                    Toast.makeText(activity, "Compra realizada", Toast.LENGTH_SHORT).show()

                    //Analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO){
                        val productos = mutableListOf<Bundle>()
                        order.products.forEach {
                            if (it.value.quantity > 5){
                                val bundle = Bundle()
                                bundle.putString("id_producto", it.key)
                                productos.add(bundle)
                            }
                        }
                        param(FirebaseAnalytics.Param.QUANTITY, productos.toTypedArray())
                    }
                    firebaseAnalytics.setUserProperty(Constants.USER_PROP_QUANTITY,
                        if (productos.size > 0) "con_mayoreo" else "sin_mayoreo")
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al comprar", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUi(true)
                }
        }
    }

    private fun getProductos(){
        (activity as? MainAux)?.getProductsCart()?.forEach {
            adapter.addProduct(it)
        }
    }

    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
    }


    private fun enableUi(enable:Boolean){
        binding?.let {
            it.imgBtnCancel.isEnabled = enable
            it.extendFab.isEnabled = enable
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.updateTotal()
        super.onDestroyView()
        binding = null
    }

}