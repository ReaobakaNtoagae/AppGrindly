package com.example.grindlyapp1


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.chip.Chip

class Guide : AppCompatActivity() {

    private lateinit var guideTitle: TextView
    private lateinit var guideImage: ImageView
    private lateinit var videoLink: TextView
    private lateinit var chipCategory: Chip
    private lateinit var chipDifficulty: Chip
    private lateinit var guideContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        guideTitle = findViewById(R.id.guideTitle)
        guideImage = findViewById(R.id.guideImage)
        videoLink = findViewById(R.id.videoLink)
        chipCategory = findViewById(R.id.chipCategory)
        chipDifficulty = findViewById(R.id.chipDifficulty)
        guideContent = findViewById(R.id.guideContent)


        guideTitle.text = "Hustler Micro-Academy Guide"
        chipCategory.text = "Entrepreneurship"
        chipDifficulty.text = "Intermediate"
        guideContent.text = """
            Welcome to the Hustler Micro-Academy! 
            This guide will teach you essential skills 
            for building a side hustle efficiently. 
            Make sure to follow each step carefully.
        """.trimIndent()


        videoLink.setOnClickListener {
            val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }
}
