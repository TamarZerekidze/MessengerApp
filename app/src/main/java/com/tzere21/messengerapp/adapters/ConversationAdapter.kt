package com.tzere21.messengerapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tzere21.messengerapp.databinding.ItemConvoBinding


data class Conversation(
    val id: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val userPhotoUrl: String = ""
)

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
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

    inner class ConversationViewHolder(
        private val binding: ItemConvoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.textViewUserName.text = conversation.userName
            binding.textViewLastMessage.text = conversation.lastMessage
            binding.textViewTime.text = conversation.timestamp

            binding.root.setOnClickListener {
                onConversationClick(conversation)
            }
        }
    }
}