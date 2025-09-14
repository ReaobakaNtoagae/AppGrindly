package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.grindlyapp1.R
import com.google.android.material.card.MaterialCardView

class UpdateServiceStatus : Fragment() {

    private lateinit var cardOption1: MaterialCardView
    private lateinit var cardOption2: MaterialCardView
    private lateinit var cardOption3: MaterialCardView
    private lateinit var cardOption4: MaterialCardView

    private lateinit var cardList: List<MaterialCardView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_service_status, container, false)

        cardOption1 = view.findViewById(R.id.cardOption1)
        cardOption2 = view.findViewById(R.id.cardOption2)
        cardOption3 = view.findViewById(R.id.cardOption3)
        cardOption4 = view.findViewById(R.id.cardOption4)

        cardList = listOf(cardOption1, cardOption2, cardOption3, cardOption4)

        cardList.forEach { card ->
            card.setOnClickListener {

                cardList.forEach {
                    it.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
                    it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }

                card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.theme_purple))
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_purple))
            }
        }

        return view
    }
}
