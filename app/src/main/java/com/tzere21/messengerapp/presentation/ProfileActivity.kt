package com.tzere21.messengerapp.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.data.UserRepository
import com.tzere21.messengerapp.databinding.ActivityProfileBinding
import com.tzere21.messengerapp.domain.User
import java.io.File
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tzere21.messengerapp.MainActivity


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedPhotoPath: String? = null

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.create(UserRepository(this))
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Storage permission required to access gallery", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.imageViewProfile.setOnClickListener { showImagePickerDialog() }
        binding.buttonUpdate.setOnClickListener { updateProfile() }
        binding.buttonSignOut.setOnClickListener { showLogoutDialog() }

        binding.navMessages.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.fabNewChat.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(this) { user ->
            user?.let { displayUserProfile(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonUpdate.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(this) { result ->
            result.fold(
                onSuccess = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(this, exception.message ?: "Update failed", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun displayUserProfile(user: User) {
        binding.editTextNickname.setText(user.nickname)
        binding.editTextProfession.setText(user.profession)

        loadProfileImage(user.photoUrl)
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
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.avatar_image_placeholder)
                .error(R.drawable.avatar_image_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        } else {
            val file = File(url)
            Glide.with(this)
                .load(file)
                .placeholder(R.drawable.avatar_image_placeholder)
                .error(R.drawable.avatar_image_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkStoragePermissionAndOpen()
                }
            }
            .show()
    }

    private fun checkStoragePermissionAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedPhotoPath = it.toString()

            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.avatar_image_placeholder)
                .error(R.drawable.avatar_image_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updateProfile() {
        val nickname = binding.editTextNickname.text.toString().trim()
        val profession = binding.editTextProfession.text.toString().trim()

        viewModel.updateProfile(nickname, profession, selectedPhotoPath)
        selectedPhotoPath = null
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}