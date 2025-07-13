package com.tzere21.messengerapp.presentation

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tzere21.messengerapp.MainActivity
import com.tzere21.messengerapp.adapters.UserAdapter
import com.tzere21.messengerapp.data.UserRepository
import com.tzere21.messengerapp.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var userAdapter: UserAdapter
    private var lastQuery = ""

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModel.create(UserRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupRecyclerView()
        setupSearchBar()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(emptyList())

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = userAdapter
        }
    }

    private fun setupSearchBar() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.length < lastQuery.length || query.length >= 3) {
                    lastQuery = query
                    viewModel.searchUsers(lastQuery)
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.users.observe(this) { users ->
            userAdapter.updateUsers(users)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            binding.recyclerViewUsers.visibility = if (loading) View.GONE else View.VISIBLE
            binding.textViewEmptyMessage.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.textViewEmptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerViewUsers.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }
}