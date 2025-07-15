package com.tzere21.messengerapp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.databinding.ItemUserBinding
import com.tzere21.messengerapp.domain.User
import java.io.File

class UserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textViewNickname.text = user.nickname
            binding.textViewProfession.text = user.profession

            loadProfileImage(user.photoUrl)

            binding.root.setOnClickListener { onUserClick(user) }
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
                    .into(binding.imageViewAvatar)
            } else {
                val file = File(url)
                Glide.with(binding.root.context)
                    .load(file)
                    .placeholder(R.drawable.avatar_image_placeholder)
                    .error(R.drawable.avatar_image_placeholder)
                    .circleCrop()
                    .into(binding.imageViewAvatar)
            }
        }
    }
}