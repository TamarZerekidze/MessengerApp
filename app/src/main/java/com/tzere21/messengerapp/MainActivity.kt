package com.tzere21.messengerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.tzere21.messengerapp.databinding.ActivityMainBinding
import com.tzere21.messengerapp.presentation.AuthActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.tzere21.messengerapp.adapters.ConversationAdapter
import com.tzere21.messengerapp.presentation.ProfileActivity
import com.tzere21.messengerapp.presentation.SearchActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private val auth = Firebase.auth
    private val database = Firebase.database

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
        setupFAB()
        setupSearchBar()
        loadUserConversations()
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
        conversationAdapter = ConversationAdapter(emptyList()) { conversation ->
            // TODO: Open chat activity
            Toast.makeText(this, "Open chat with ${conversation.userName}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupBottomNavigation() {
        setSelectedTab(true)

        binding.navMessages.setOnClickListener {
            setSelectedTab(true)
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setSelectedTab(isMessages: Boolean) {

    }

    private fun setupFAB() {
        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun setupSearchBar() {
        binding.editTextSearch.setOnClickListener {
            Toast.makeText(this, "Search filtering coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserConversations() {
        showEmptyState()
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewConversations.visibility = View.GONE
    }
}