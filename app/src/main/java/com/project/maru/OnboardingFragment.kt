package com.project.maru

import android.os.Bundle
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)
        val args = requireArguments()

        view.findViewById<ImageView>(R.id.ivOnboarding)
            .setImageResource(args.getInt(ARG_IMAGE))
        val title = view.findViewById<TextView>(R.id.tvOnboardingTitle)
        val titleText = getString(args.getInt(ARG_TITLE))
        val accent = args.getString(ARG_ACCENT).orEmpty()
        val accentStart = titleText.lastIndexOf(accent)
        title.text = SpannableString(titleText).apply {
            if (accentStart >= 0) setSpan(
                ForegroundColorSpan(Color.rgb(255, 148, 148)),
                accentStart,
                accentStart + accent.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        view.findViewById<TextView>(R.id.tvOnboardingDescription)
            .setText(args.getInt(ARG_DESCRIPTION))

        return view
    }

    companion object {
        private const val ARG_IMAGE = "image"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_ACCENT = "accent"

        fun newInstance(imageRes: Int, titleRes: Int, descriptionRes: Int, accent: String) =
            OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE, imageRes)
                    putInt(ARG_TITLE, titleRes)
                    putInt(ARG_DESCRIPTION, descriptionRes)
                    putString(ARG_ACCENT, accent)
                }
            }
    }
}
