package com.example.detail

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilo.R
import com.google.firebase.storage.StorageReference

class DetailAdapter(
    private val imageList:MutableList<StorageReference>,
    private val context: Context)
    : PagerAdapter() {

    override fun getCount(): Int = imageList.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imgProducto = ImageView(context)

        GlideApp
            .with(context)
            .load(imageList[position])
            .placeholder(R.drawable.time_lapse)
            .error(R.drawable.broken_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imgProducto)

        container.addView(imgProducto, 0)
        return  imgProducto
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ImageView)
    }
}