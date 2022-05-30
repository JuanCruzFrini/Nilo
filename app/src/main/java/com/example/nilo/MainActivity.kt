package com.example.nilo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cart.CartFragment
import com.example.detail.DetailFragment
import com.example.nilo.databinding.ActivityMainBinding
import com.example.order.OrderActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), OnProductListener,MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    private var productoSelected:Producto? = null
    val proudctCartList = mutableListOf<Producto>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()
        setRecyclerView()
        configButtons()

        //FCM, asi se consulta el token manualmente
        //consultarToken()
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            //si hay un usuario activo
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.NestScrollView.show()
                binding.progressLayout.hide()
            } else {
                //si no hay un usuario activo, muestra FirebaseUI login.
                //proveedores de login
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                //creamos el UI de autenticacion/login
                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build()
                )
            }
        }
    }

    //launcher para autenticacion/login
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { ActivityResult ->
            val response = IdpResponse.fromResultIntent(ActivityResult.data)

            if (ActivityResult.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()

                    //obtenemos el token de usuario
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val token = preferences.getString(Constants.PROP_TOKEN, null)
                    token?.let {
                        val db = FirebaseFirestore.getInstance()
                        val tokenMap = hashMapOf(Pair(Constants.PROP_TOKEN, token))
                        db.collection(Constants.COLL_USERS)
                            .document(user.uid)
                            .collection(Constants.COLL_TOKENS)
                            .add(tokenMap)
                            .addOnSuccessListener {
                                Log.i("registered token", token)
                                preferences.edit {
                                    putString(Constants.PROP_TOKEN, null)
                                        .apply()
                                }
                            }
                            .addOnFailureListener {
                                Log.i("not registered token", token)
                            }
                    }
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "Hasta luego", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                } else {
                    response.error?.let { firebaseUiException ->
                        if (firebaseUiException.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "Sin Internet", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Error: ${firebaseUiException.errorCode}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    private fun setRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.let {
            it.layoutManager = GridLayoutManager(this@MainActivity, 3)
            it.adapter = adapter
        }
    }

    private fun configButtons() {
        binding.btnViewCart.setOnClickListener {
            val fragment = CartFragment()
            fragment.show(
                supportFragmentManager.beginTransaction(),
                CartFragment::class.java.simpleName
            )
        }
    }

    //FCM
    fun consultarToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.i("get token", token.toString())
            } else {
                Log.i("get token fail", task.exception.toString())
            }
        }
    }

    //lee listado de firebase en y lo actualiza en tiempo real
    private fun configFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTOS)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges) {
                val producto = snapshot.document.toObject(Producto::class.java)
                producto.id = snapshot.document.id
                when (snapshot.type) {
                    DocumentChange.Type.ADDED -> adapter.addProduct(producto)
                    DocumentChange.Type.MODIFIED -> adapter.updateProduct(producto)
                    DocumentChange.Type.REMOVED -> adapter.deleteProduct(producto)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    //menu overflow
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> cerrarSesion()
            R.id.action_order_history -> openHistory()
        }
        return true
    }

    private fun openHistory() {
        startActivity(Intent(this, OrderActivity::class.java))
    }

    private fun cerrarSesion() {
        AuthUI.getInstance().signOut(this)
            .addOnSuccessListener {
                Toast.makeText(this, "Sesion terminada", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    binding.progressLayout.show()
                    binding.NestScrollView.hide()
                } else {
                    Toast.makeText(this, "No se pudo cerrar la sesion", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onClick(producto: Producto) {
        val index = proudctCartList.indexOf(producto)
        if (index != -1){
            productoSelected = proudctCartList[index]
        } else {
            productoSelected = producto
            //proudctCartList.add(producto)
        }

        val fragment = DetailFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container_main, fragment)
            .addToBackStack(null)
            .commit()

        showButton(false)
    }

    override fun getProductsCart(): MutableList<Producto> = proudctCartList

    override fun getProductoSelected(): Producto? = productoSelected

    override fun showButton(isVisible: Boolean) {
        binding.btnViewCart.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun addProductoToCart(producto: Producto) {
        val index = proudctCartList.indexOf(producto)
        if (index != -1){
            proudctCartList.set(index, producto)
        } else {
            proudctCartList.add(producto)
        }
        updateTotal()
    }

    override fun updateTotal() {
        var total = 0.0
        proudctCartList.forEach { producto->
            total += producto.totalPrice()
        }
        if (total == 0.0){
            binding.txtTotal.text = getString(R.string.producto_empty_cart)
        } else {
            binding.txtTotal.text = getString(R.string.producto_full_cart, total)
        }
    }

    override fun clearCart() {
        proudctCartList.clear()
    }
}
fun View.hide() { visibility = View.GONE }
fun View.show() { visibility = View.VISIBLE }

fun View.enable() { this.isEnabled = true}
fun View.disable() {this.isEnabled = false}