package com.capztone.fishfy.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capztone.fishfy.databinding.SlideItemContainerBinding
import com.capztone.fishfy.ui.activities.models.IntroSlide

class IntroSliderAdapter(private val introSlides: List<IntroSlide>)
    : RecyclerView.Adapter<IntroSliderAdapter.IntroSlideViewHolder>() {
    //for adding text to speech listener in the onboarding fragment
    var onTextPassed: ((textView: TextView) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroSlideViewHolder {
        return IntroSlideViewHolder(
            SlideItemContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return introSlides.size
    }

    override fun onBindViewHolder(holder: IntroSlideViewHolder, position: Int) {
        holder.bind(introSlides[position])
    }

    inner class IntroSlideViewHolder(private val binding: SlideItemContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(introSlide: IntroSlide) {
            binding.textTitle.text = introSlide.title
            binding.textDescription.text = introSlide.description

            // Assuming you have the image resources in the drawable folder
            val context = binding.imageSlideIcon.context
            val resourceId = context.resources.getIdentifier(
                introSlide.icon.replace(".jpg", ""),
                "drawable",
                context.packageName
            )
            binding.imageSlideIcon.setImageResource(resourceId)

            onTextPassed?.invoke(binding.textTitle)
        }
    }
}