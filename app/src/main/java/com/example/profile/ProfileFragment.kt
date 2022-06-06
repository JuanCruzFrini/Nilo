package com.example.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.Constants
import com.example.nilo.MainAux
import com.example.nilo.R
import com.example.nilo.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var binding:FragmentProfileBinding? = null

    private var photoSelectedUri:Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater,container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configButtons()
        getUser()
    }

    private fun getUser() {
        binding?.let { binding ->
            FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->
                binding.etFullName.setText(firebaseUser.displayName)
                //binding.etPhotoUrl.setText(firebaseUser.photoUrl.toString())

                Glide
                    .with(this)
                    .load(firebaseUser.photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.broken_image)
                    .placeholder(R.drawable.time_lapse)
                    .centerCrop()
                    .circleCrop()
                    .into(binding.imgBtnProfile)

                setUpActionBar()
            }
        }
    }

    private fun setUpActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.profile_title)
            setHasOptionsMenu(true)
        }
    }

    //damos funcionalidad al boton de retroceso en la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configButtons() {
        binding?.let { binding->
            binding.imgBtnProfile.setOnClickListener {
                openGallery()
            }
            binding.btnUpdate.setOnClickListener {
                binding.etFullName.clearFocus()

                FirebaseAuth.getInstance().currentUser?.let { user ->
                    if (photoSelectedUri == null){
                        updateUserProfile(binding, user, Uri.EMPTY)
                    } else {
                        uploadReducedImage(user)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun updateUserProfile(binding: FragmentProfileBinding, user: FirebaseUser, uri: Uri) {
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etFullName.text.toString().trim())
            .setPhotoUri(uri)
            .build()

        user.updateProfile(profileUpdate)
            .addOnSuccessListener {
                Toast.makeText(activity, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                (activity as? MainAux)?.updateTitle(user)
                activity?.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al actualizar el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    //sube la imagen seleccionada y reducida a Firebase Storage
    private fun uploadReducedImage(user: FirebaseUser) {
        //creamos una carpeta de imagenes por usuario autenticado, con su UID
        val profileRef = FirebaseStorage.getInstance().reference.child(user.uid)
            .child(Constants.PATH_PROFILE).child(Constants.MY_PHOTO)

        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                getBitmapFromUri(uri)?.let { bitmap ->
                    binding.progressBar.show()

                    //reduce el peso de la imagen a subir
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)

                    //sube la imagen en la calidad por default
                    //photoRef.putFile(uri)
                    //sube la imagen comprimida
                    profileRef.putBytes(baos.toByteArray())
                        .addOnProgressListener {
                            val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                            it.run {
                                binding.progressBar.progress = progress
                                binding.txtProgress.text = String.format("%s%%", progress)
                            }
                        }
                        .addOnCompleteListener {
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.txtProgress.text = ""
                        }
                        .addOnSuccessListener {
                            it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                Log.i("URL", downloadUrl.toString())
                                updateUserProfile(binding, user, downloadUrl)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                activity,
                                "Error al subir imagen ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }
    }


    //create bitmap para luego reducir tamaño
    private fun getBitmapFromUri(uri:Uri) : Bitmap?{
        activity?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(it.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(it.contentResolver,uri)
            }
            return getResizedImage(bitmap, 320)
            //return bitmap
        }
        return null
    }

    //redimension de imagen (sin usar pq deforma mucho la foto)
    private fun getResizedImage(image: Bitmap, maxSize:Int) : Bitmap {
        var width = image.width
        var height = image.height
        if (width <= maxSize && height <= maxSize) return  image

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1){
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else{
            height = maxSize
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            photoSelectedUri = it.data?.data

            binding?.let {
                Glide
                    .with(this)
                    .load(photoSelectedUri)
                    .error(R.drawable.broken_image)
                    .placeholder(R.drawable.time_lapse)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .circleCrop()
                    .into(it.imgBtnProfile)
            }
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}