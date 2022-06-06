package com.example.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.*
import com.example.nilo.databinding.FragmentDetailBinding
import com.google.firebase.storage.FirebaseStorage

class DetailFragment : Fragment() {
    private var binding: FragmentDetailBinding? = null
    private var producto:Producto? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getProducto()
        setUpButtons()
    }

    //cargar el detalle del producto
    private fun getProducto() {
       producto = (activity as? MainAux)?.getProductoSelected()
        producto?.let { producto->
            binding?.let { binding ->
                binding.txtName.text = producto.name
                binding.txtDescripcion.text = producto.description
                //binding.txtQuantity.append(producto.quantity.toString())
                binding.txtQuantity.text = getString(R.string.detail_quantity, producto.quantity)
                setNewQuantity(producto)

                if (!producto.sellerId.isNullOrEmpty()){
                    context?.let { context ->
                        val productoRef = FirebaseStorage.getInstance().reference
                            .child(producto.sellerId)
                            .child(Constants.PATH_IMAGE)
                            .child(producto.id!!)

                        productoRef.listAll()
                            .addOnSuccessListener { imageList ->
                                val detailAdapter = DetailAdapter(imageList.items, context)
                                binding.viewPagerProducto.apply {
                                    adapter = detailAdapter
                                }
                            }
                    }
                } else {
                    //arreglar constraint
                    binding.imgProducto.show()
                    binding.viewPagerProducto.hide()
                    Glide.with(requireContext())
                        .load(producto.imgUrl)
                        .placeholder(R.drawable.time_lapse)
                        .error(R.drawable.broken_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.imgProducto)
                }
            }
        }
    }
    private fun setNewQuantity(producto: Producto){
        binding?.let {
            it.etNewQuantity.setText(producto.newQuantity.toString())
            //este recurso string puede recibir parametros
            val newQuantityStr = getString(
                R.string.detail_total_price,
                producto.totalPrice(),
                producto.newQuantity, producto.price)
            //con esto le añadimos estilo HTML al string
            it.txtTotalPrice.text = HtmlCompat.fromHtml(newQuantityStr, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun setUpButtons(){
        producto?.let { producto ->
            binding?.let { binding ->
                binding.imgBtnRest.setOnClickListener {
                    if (producto.newQuantity > 1){
                        producto.newQuantity -= 1
                        setNewQuantity(producto)
                    }
                }
                binding.imgBtnSum.setOnClickListener {
                    if (producto.newQuantity < producto.quantity){
                        producto.newQuantity += 1
                        setNewQuantity(producto)
                    }
                }
                binding.extendFab.setOnClickListener {
                    producto.newQuantity = binding.etNewQuantity.text.toString().toInt()
                    addToCart(producto)
                }
            }
        }
    }
    private fun addToCart(producto: Producto){
        (activity as? MainAux)?.let {
            it.addProductoToCart(producto)
            activity?.onBackPressed()
        }
    }

    //desvinculamos el binding y volvemos a mostrar el button "Ver carrito"
    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }


}