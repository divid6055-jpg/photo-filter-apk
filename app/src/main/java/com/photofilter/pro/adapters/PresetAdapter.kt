package com.photofilter.pro.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.photofilter.pro.databinding.ItemPresetBinding
import com.photofilter.pro.utils.FilterPreset
import com.photofilter.pro.utils.PresetsManager

/**
 * محول قائمة الـ Presets في Bottom Sheet.
 */
class PresetAdapter(
    private val onClick: (FilterPreset) -> Unit
) : RecyclerView.Adapter<PresetAdapter.PresetVH>() {

    private val presets = PresetsManager.presets

    // وصف قصير لكل preset
    private val presetDescriptions = mapOf(
        "auto_enhance" to "تحسين تلقائي للألوان والحدة",
        "portrait" to "ألوان دافئة، سطوع ناعم، حدة معتدلة",
        "landscape" to "ألوان زاهية، تباين عالٍ، تفاصيل دقيقة",
        "food" to "ألوان ذهبية، تشبع عالٍ للطعام",
        "night_mode" to "رفع الظلال، تقليل الضوضاء",
        "vintage_film" to "مظهر فيلم قديم مع حبيبات",
        "bw_dramatic" to "أبيض وأسود درامي بتباين عالٍ",
        "cinematic" to "مظهر سينمائي مع ظلال عميقة"
    )

    // أيقونة لكل preset
    private val presetIcons = mapOf(
        "auto_enhance" to com.photofilter.pro.R.drawable.ic_auto_fix,
        "portrait" to com.photofilter.pro.R.drawable.ic_portrait,
        "landscape" to com.photofilter.pro.R.drawable.ic_landscape,
        "food" to com.photofilter.pro.R.drawable.ic_food,
        "night_mode" to com.photofilter.pro.R.drawable.ic_night,
        "vintage_film" to com.photofilter.pro.R.drawable.ic_vintage,
        "bw_dramatic" to com.photofilter.pro.R.drawable.ic_bw,
        "cinematic" to com.photofilter.pro.R.drawable.ic_film
    )

    inner class PresetVH(val binding: ItemPresetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetVH {
        val binding = ItemPresetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PresetVH(binding)
    }

    override fun getItemCount(): Int = presets.size

    override fun onBindViewHolder(holder: PresetVH, position: Int) {
        val preset = presets[position]
        with(holder.binding) {
            tvPresetName.setText(preset.displayNameRes)
            tvPresetDesc.text = presetDescriptions[preset.name] ?: ""
            ivPresetIcon.setImageResource(presetIcons[preset.name] ?: com.photofilter.pro.R.drawable.ic_presets)
            root.setOnClickListener { onClick(preset) }
        }
    }
}
