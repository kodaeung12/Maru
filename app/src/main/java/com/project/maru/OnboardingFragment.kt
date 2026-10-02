package com.project.maru

import android.os.Bundle
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
        view.findViewById<TextView>(R.id.tvOnboardingTitle)
            .setText(args.getInt(ARG_TITLE))
        view.findViewById<TextView>(R.id.tvOnboardingDescription)
            .setText(args.getInt(ARG_DESCRIPTION))

        return view
    }

    companion object {
        private const val ARG_IMAGE = "image"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(imageRes: Int, titleRes: Int, descriptionRes: Int) =
            OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE, imageRes)
                    putInt(ARG_TITLE, titleRes)
                    putInt(ARG_DESCRIPTION, descriptionRes)
                }
            }
    }
}
