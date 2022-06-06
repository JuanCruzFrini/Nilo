package com.example.promo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.MainAux
import com.example.nilo.R
import com.example.nilo.databinding.FragmentPromoBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

class PromoFragment : Fragment() {

    private var binding:FragmentPromoBinding? = null

    private var mainTitle = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPromoBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configRemoteConfig()
        configActionBar()
    }

    private fun configActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = it.supportActionBar?.title.toString()
            it.supportActionBar?.title = "Promociones"
            setHasOptionsMenu(true)
        }
    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_default)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val percentaje = remoteConfig.getDouble("percentaje")
                    val photoUrl = remoteConfig.getString("photoUrl")
                    val message = remoteConfig.getString("message")

                    binding?.let {
                        it.txtMessage.text = message
                        it.txtPercentaje.text = percentaje.toString()

                        Glide.with(requireContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.time_lapse)
                            .error(R.drawable.ofertas_icon)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(it.imgProducto)
                    }
                }
            }
    }

    //damos funcionalidad al boton de retroceso en la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = mainTitle
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}