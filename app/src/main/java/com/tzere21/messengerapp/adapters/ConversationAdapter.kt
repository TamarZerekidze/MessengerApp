package com.tzere21.messengerapp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzere21.messengerapp.R
import com.tzere21.messengerapp.databinding.ItemConvoBinding
import com.tzere21.messengerapp.domain.UserChat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private var conversations: List<UserChat>,
    private val onConversationClick: (UserChat) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConvoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateConversations(newConversations: List<UserChat>) {
        conversations = newConversations
        notifyDataSetChanged()
    }

    inner class ConversationViewHolder(
        private val binding: ItemConvoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: UserChat) {
            binding.textViewUserName.text = conversation.secondUserName
            binding.lastMessage.text = conversation.lastMessage
            binding.textViewTime.text = calculateTimeAfter(conversation.timestamp)

            loadProfileImage(conversation.secondUserPhotoUrl)

            binding.root.setOnClickListener {
                onConversationClick(conversation)
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

        private fun calculateTimeAfter(timestamp: Long) : String {
            val now = System.currentTimeMillis()
            val diff = (now - timestamp) / 60000

            return when {
                diff < 60 -> "$diff min"
                diff < 2 * 60 -> "1 hour"
                diff < 24 * 60 -> "${(diff / 60)} hours"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}