package com.tzere21.messengerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.tzere21.messengerapp.databinding.ActivityMainBinding
import com.tzere21.messengerapp.presentation.AuthActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.tzere21.messengerapp.adapters.ConversationAdapter
import com.tzere21.messengerapp.data.UserChatRepository
import com.tzere21.messengerapp.presentation.ChatActivity
import com.tzere21.messengerapp.presentation.ProfileActivity
import com.tzere21.messengerapp.presentation.SearchActivity
import com.tzere21.messengerapp.presentation.UserChatViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private val auth = Firebase.auth
    private val database = Firebase.database

    private val viewModel: UserChatViewModel by viewModels {
        UserChatViewModel.create(UserChatRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        checkAuthentication()
        setupRecyclerView()
        setupBottomNavigation()
        setupSearchBar()
        observeViewModel()
    }
    private fun checkAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        Log.d("MainActivity", "User logged in: ${currentUser.email}")

        loadCurrentUserInfo()
    }

    private fun loadCurrentUserInfo() {
        val currentUser = auth.currentUser ?: return

        database.getReference("users").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error loading user info", error.toException())
                }
            })
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            emptyList()
        ) { conversation ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("USER_UID", conversation.secondUserId)
                putExtra("USER_NICKNAME", conversation.secondUserName)
                putExtra("USER_EMAIL", conversation.secondUserEmail)
                putExtra("USER_PROFESSION", conversation.secondUserProfession)
                putExtra("USER_PHOTO_URL", conversation.secondUserPhotoUrl)
            }
            startActivity(intent)
        }

        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun setupSearchBar() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewModel.searchUserChats(query)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.userChats.observe(this) { userChats ->
            conversationAdapter.updateConversations(userChats)

            if (userChats.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewConversations.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerViewConversations.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            binding.layoutEmptyState.visibility = if (loading) View.GONE else View.VISIBLE
            binding.recyclerViewConversations.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}