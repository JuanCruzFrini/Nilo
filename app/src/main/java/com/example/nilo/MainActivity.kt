package com.example.nilo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Base64
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
import com.example.profile.ProfileFragment
import com.example.promo.PromoFragment
import com.example.settings.SettingsActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.security.MessageDigest

class MainActivity : AppCompatActivity(), OnProductListener,MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration
    private var queryPagination:Query? = null

    private var productoSelected:Producto? = null
    val proudctCartList = mutableListOf<Producto>()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()
        setRecyclerView()
        configButtons()
        configAnalytics()
        configRemoteConfig()
        configToolbar()

        //FCM, asi se consulta el token manualmente
        //consultarToken()
    }

    private fun configToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings{
            minimumFetchIntervalInSeconds = 5 // 3600 = 1hs en segundos
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.setDefaultsAsync(R.xml.remote_config_default)

        remoteConfig.fetchAndActivate()
           /* .addOnSuccessListener {
                Snackbar.make(binding.root, "Datos locales/remotos", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Datos locales", Snackbar.LENGTH_SHORT).show()
            }*/
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val isPromoDay = remoteConfig.getBoolean("isPromoDay")
                    val promCounter = remoteConfig.getLong("promCounter")
                    val percentaje = remoteConfig.getDouble("percentaje")
                    val photoUrl = remoteConfig.getString("photoUrl")
                    val message = remoteConfig.getString("message")

                    if (isPromoDay){
                        val badge = BadgeDrawable.create(this)
                        BadgeUtils.attachBadgeDrawable(badge, binding.toolbar, R.id.action_promo)//bug
                        badge.number = promCounter.toInt()
                    }
                }
            }
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            //si hay un usuario activo
            if (auth.currentUser != null) {
                //supportActionBar?.title = auth.currentUser?.displayName
                updateTitle(auth.currentUser!!)
                binding.NestScrollView.show()
                binding.progressLayout.hide()
            } else {
                //si no hay un usuario activo, muestra FirebaseUI login.
                //proveedores de login
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build()
                    /*AuthUI.IdpConfig.PhoneBuilder().build()*/
                )

                //Layout personalizada para el login
                val loginView = AuthMethodPickerLayout
                    .Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.btnEmail)
                    .setGoogleButtonId(R.id.btnGoogle)
                    .setFacebookButtonId(R.id.btnFacebook)
                    .setTosAndPrivacyPolicyId(R.id.txtPolicy)
                    .build()

                //creamos el UI de autenticacion/login
                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls("https://github.com","https://github.com")
                        .setAuthMethodPickerLayout(loginView)
                        .setTheme(R.style.LoginTheme)
                        .build()
                )
            }
        }
        //try catch exclusivo para Facebook auth
        //lo pide el paso #4, nos imprime la clave hash necesaria para el mismo(developers.facebook.com)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo("com.example.nilo", PackageManager.GET_SIGNING_CERTIFICATES)
                for (signature in info.signingInfo.apkContentsSigners) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API >= 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            } else {
                val info = packageManager.getPackageInfo("com.example.nilo", PackageManager.GET_SIGNATURES);
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API < 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        adapter = ProductAdapter(mutableListOf(Producto()), this)
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

    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
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

        firestoreListener = productRef
            .limit(6) //pagination
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshots?.let { items->
                    //pagination
                    val lastItem = items.documents[items.size() - 1]
                    queryPagination = productRef
                        .startAfter(lastItem)
                        .limit(6)

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
            R.id.action_profile -> openProfile()
            R.id.action_settings -> openSettings()
            R.id.action_promo -> openPromos()

        }
        return true
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openProfile() {
        val fragment = ProfileFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container_main, fragment)
            .addToBackStack(null)
            .commit()
        showButton(false)
    }

    private fun openHistory() {
        startActivity(Intent(this, OrderActivity::class.java))
    }

    private fun openPromos() {
        val fragment = PromoFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container_main, fragment)
            .addToBackStack(null)
            .commit()
        showButton(false)
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

        //Analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
            param(FirebaseAnalytics.Param.ITEM_ID, producto.id!!)
            param(FirebaseAnalytics.Param.ITEM_NAME, producto.name!!)
        }
    }

    //pagination
    override fun loadMore() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTOS)

        queryPagination?.let {
            it.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshots?.let { items ->
                    //pagination
                    val lastItem = items.documents[items.size() - 1]
                    queryPagination = productRef
                        .startAfter(lastItem)
                        .limit(6)

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
        }
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

    override fun updateTitle(user: FirebaseUser) {
        supportActionBar?.title = user.displayName
    }
}
fun View.hide() { visibility = View.GONE }
fun View.show() { visibility = View.VISIBLE }

fun View.enable() { this.isEnabled = true}
fun View.disable() {this.isEnabled = false}