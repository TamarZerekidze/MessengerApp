package com.tzere21.messengerapp.presentation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.databinding.ActivityMainBinding
import com.tzere21.messengerapp.databinding.ActivityRegisterBinding
import com.tzere21.messengerapp.domain.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = Firebase.auth
    private val database = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonSignUp.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val nickname = binding.editTextNick.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val profession = binding.editTextProfession.text.toString().trim()

        if (nickname.isEmpty() || password.isEmpty() || profession.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        checkNicknameExists(nickname) { exists ->
            if (exists) {
                showLoading(false)
                Toast.makeText(this, "Nickname already exists", Toast.LENGTH_SHORT).show()
                return@checkNicknameExists
            }

            val email = "$nickname@messenger.app"

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = User(
                            uid = auth.currentUser?.uid ?: "",
                            nickname = nickname,
                            email = email,
                            profession = profession
                        )

                        saveUserToDatabase(user)
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkNicknameExists(nickname: String, callback: (Boolean) -> Unit) {
        val usersRef = database.getReference("users")
        usersRef.orderByChild("nickname").equalTo(nickname)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    private fun saveUserToDatabase(user: User) {
        database.getReference("users").child(user.uid).setValue(user)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Failed to save user data: ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonSignUp.isEnabled = !show
    }
}