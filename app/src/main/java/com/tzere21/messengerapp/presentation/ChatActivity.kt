package com.tzere21.messengerapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.tzere21.messengerapp.MainActivity
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.databinding.ActivityChatBinding
import com.tzere21.messengerapp.domain.User
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = User(
            uid = intent.getStringExtra("USER_UID") ?: "",
            nickname = intent.getStringExtra("USER_NICKNAME") ?: "",
            email = intent.getStringExtra("USER_EMAIL") ?: "",
            profession = intent.getStringExtra("USER_PROFESSION") ?: "",
            photoUrl = intent.getStringExtra("USER_PHOTO_URL") ?: ""
        )

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.collapsingHeader.title = user.nickname

        binding.userNickname.text = user.nickname
        binding.userProfession.text = user.profession
        loadProfileImage(user.photoUrl)
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        var url = imageUrl
        if (url.isEmpty()) {
            url = "https://www.computerhope.com/jargon/g/guest-user.png"
        }
        if (!url.startsWith("http") && !File(url).exists()) {
            url = "https://www.computerhope.com/jargon/g/guest-user.png"
        }

        if (url.startsWith("http")) {
            Glide.with(binding.root.context)
                .load(url)
                .placeholder(R.drawable.avatar_image_placeholder)
                .error(R.drawable.avatar_image_placeholder)
                .circleCrop()
                .into(binding.userAvatar)
        } else {
            val file = File(url)
            Glide.with(binding.root.context)
                .load(file)
                .placeholder(R.drawable.avatar_image_placeholder)
                .error(R.drawable.avatar_image_placeholder)
                .circleCrop()
                .into(binding.userAvatar)
        }
    }
}