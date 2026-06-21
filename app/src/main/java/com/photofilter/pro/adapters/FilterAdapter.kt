package com.photofilter.pro.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.photofilter.pro.databinding.ItemFilterBinding
import com.photofilter.pro.utils.FilterType
import com.photofilter.pro.utils.ImageProcessor

/**
 * محول قائمة الفلاتر الأفقية.
 * يحمل نسخة مصغّرة من الصورة الأصلية ويولّد معاينة لكل فلتر.
 */
class FilterAdapter(
    private val sourceBitmap: Bitmap,
    private val onClick: (FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterVH>() {

    private val filters = FilterType.entries.toList()
    private var selectedFilter: FilterType = FilterType.NONE

    // معاينة مصغّرة مشتركة (60dp) لكل الفلاتر - تُولّد lazily
    private val thumbBitmap: Bitmap by lazy {
        val maxSize = 200
        val scale = if (sourceBitmap.width > sourceBitmap.height) {
            maxSize.toFloat() / sourceBitmap.width
        } else {
            maxSize.toFloat() / sourceBitmap.height
        }
        Bitmap.createScaledBitmap(
            sourceBitmap,
            (sourceBitmap.width * scale).toInt().coerceAtLeast(1),
            (sourceBitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    // كاش للمعاينات
    private val previewCache = HashMap<FilterType, Bitmap>()

    inner class FilterVH(val binding: ItemFilterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterVH {
        val binding = ItemFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterVH(binding)
    }

    override fun getItemCount(): Int = filters.size

    override fun onBindViewHolder(holder: FilterVH, position: Int) {
        val filter = filters[position]
        with(holder.binding) {
            tvName.text = getFilterDisplayName(filter, root.context)

            val preview = previewCache.getOrPut(filter) {
                if (filter == FilterType.NONE) {
                    thumbBitmap
                } else {
                    try {
                        ImageProcessor.applyFilter(thumbBitmap, filter)
                    } catch (e: Exception) {
                        thumbBitmap
                    }
                }
            }
            ivPreview.setImageBitmap(preview)

            viewSelected.visibility = if (filter == selectedFilter) View.VISIBLE else View.GONE
            tvName.setTextColor(
                if (filter == selectedFilter) {
                    androidx.core.content.ContextCompat.getColor(root.context, com.photofilter.pro.R.color.brand_primary)
                } else {
                    androidx.core.content.ContextCompat.getColor(root.context, com.photofilter.pro.R.color.brand_text_secondary)
                }
            )

            root.setOnClickListener {
                val oldPos = filters.indexOf(selectedFilter)
                selectedFilter = filter
                if (oldPos >= 0) notifyItemChanged(oldPos)
                notifyItemChanged(position)
                onClick(filter)
            }
        }
    }

    fun setSelectedFilter(filter: FilterType) {
        if (filter == selectedFilter) return
        val oldPos = filters.indexOf(selectedFilter)
        selectedFilter = filter
        val newPos = filters.indexOf(filter)
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    private fun getFilterDisplayName(
        filter: FilterType,
        context: android.content.Context
    ): String {
        val resId = when (filter) {
            FilterType.NONE -> com.photofilter.pro.R.string.filter_none
            FilterType.VINTAGE -> com.photofilter.pro.R.string.filter_vintage
            FilterType.BLACK_WHITE -> com.photofilter.pro.R.string.filter_bw
            FilterType.SEPIA -> com.photofilter.pro.R.string.filter_sepia
            FilterType.COOL -> com.photofilter.pro.R.string.filter_cool
            FilterType.WARM -> com.photofilter.pro.R.string.filter_warm
            FilterType.VIVID -> com.photofilter.pro.R.string.filter_vivid
            FilterType.FADE -> com.photofilter.pro.R.string.filter_fade
            FilterType.NOIR -> com.photofilter.pro.R.string.filter_noir
            FilterType.CINEMATIC -> com.photofilter.pro.R.string.filter_cinematic
            FilterType.DRAMA -> com.photofilter.pro.R.string.filter_drama
            FilterType.SILVER -> com.photofilter.pro.R.string.filter_silver
            FilterType.LOMO -> com.photofilter.pro.R.string.filter_lomo
            FilterType.POLAROID -> com.photofilter.pro.R.string.filter_polaroid
            FilterType.SUNRISE -> com.photofilter.pro.R.string.filter_sunrise
            FilterType.NIGHT -> com.photofilter.pro.R.string.filter_night
            FilterType.SKETCH -> com.photofilter.pro.R.string.filter_sketch
            FilterType.INVERT -> com.photofilter.pro.R.string.filter_invert
        }
        return context.getString(resId)
    }

    fun release() {
        // نظّف المعاينات المُولّدة (وليس thumbBitmap الأصلية)
        previewCache.forEach { (_, bmp) ->
            if (bmp != thumbBitmap && !bmp.isRecycled) bmp.recycle()
        }
        previewCache.clear()
    }
}
