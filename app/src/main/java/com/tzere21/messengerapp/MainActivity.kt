package com.tzere21.messengerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.tzere21.messengerapp.databinding.ActivityMainBinding
import com.tzere21.messengerapp.presentation.AuthActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkAuthentication()

        testFirebaseConnection()
    }
    private fun checkAuthentication() {
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        } else {
            Log.d("MainActivity", "User logged in: ${currentUser.email}")

        }
    }

    private fun testFirebaseConnection() {
        val database = Firebase.database
        val testRef = database.getReference("test")

        testRef.setValue("Messenger app connected!")
            .addOnSuccessListener {
                Log.d("Firebase", "Database connection successful!")
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Database connection failed", exception)
            }
    }
}