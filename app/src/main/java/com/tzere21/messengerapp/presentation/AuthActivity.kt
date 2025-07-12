package com.tzere21.messengerapp.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.tzere21.messengerapp.MainActivity
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.databinding.ActivityAuthBinding
import com.tzere21.messengerapp.domain.User

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val auth = Firebase.auth
    private val database = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonSignIn.setOnClickListener {
            signIn()
        }

        binding.buttonSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signIn() {
        val nickname = binding.editTextNick.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        if (nickname.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        database.getReference("users").orderByChild("nickname").equalTo(nickname)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.firstOrNull()
                    val user = userSnapshot?.getValue(User::class.java)

                    user?.let { userData ->
                        auth.signInWithEmailAndPassword(userData.email, password)
                            .addOnCompleteListener(this) { task ->
                                showLoading(false)
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Invalid nickname or password",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                    } ?: run {
                        showLoading(false)
                        Toast.makeText(this, "User data error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Nickname not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e("AuthActivity", "Database query failed", exception)
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonSignIn.isEnabled = !show
        binding.buttonSignUp.isEnabled = !show
    }
}