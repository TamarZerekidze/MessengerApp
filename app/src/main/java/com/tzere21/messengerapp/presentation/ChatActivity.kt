package com.tzere21.messengerapp.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.firebase.Firebase
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.auth
import com.tzere21.messengerapp.MainActivity
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.adapters.ChatAdapter
import com.tzere21.messengerapp.data.ChatRepository
import com.tzere21.messengerapp.databinding.ActivityChatBinding
import com.tzere21.messengerapp.domain.User
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var user: User
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel

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
        viewModel = ViewModelProvider(
            this,
            ChatViewModel.create(
                ChatRepository(),
                user.uid)
        )[ChatViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.collapsingHeader.title = user.nickname

        binding.userNickname.text = user.nickname
        binding.userProfession.text = user.profession
        loadProfileImage(user.photoUrl)
    }

    private fun setupRecyclerView() {
        val currentUserId = Firebase.auth.currentUser?.uid ?: ""
        chatAdapter = ChatAdapter(emptyList(), currentUserId)

        binding.messages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.buttonSend.setOnClickListener {
            val messageText = binding.textMessage.text.toString()
            if (messageText.trim().isNotEmpty()) {
                viewModel.sendMessage(messageText)
                binding.textMessage.setText("")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)

            if (messages.isEmpty()) {
                binding.emptyChatMessage.visibility = View.VISIBLE
                binding.messages.visibility = View.GONE
            } else {
                binding.emptyChatMessage.visibility = View.GONE
                binding.messages.visibility = View.VISIBLE
                binding.messages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            binding.emptyChatMessage.visibility = if (loading) View.GONE else View.VISIBLE
            binding.messages.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
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