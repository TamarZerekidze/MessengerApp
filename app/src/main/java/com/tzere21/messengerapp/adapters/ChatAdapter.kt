package com.tzere21.messengerapp.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tzere21.messengerapp.databinding.ItemSentMessageBinding
import com.tzere21.messengerapp.databinding.ItemReceivedMessageBinding
import com.tzere21.messengerapp.domain.Message

class ChatAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            val binding = ItemSentMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SentMessageViewHolder(binding)
        }
        val binding = ItemReceivedMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceivedMessageViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        if (messages[position].senderId == currentUserId) {
            return 0
        }
        return 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var shouldAddPadding = false
        if (position > 0 && messages[position - 1].senderId != messages[position].senderId) {
            shouldAddPadding = true
        }

        if (holder is SentMessageViewHolder) {
            holder.bind(messages[position], shouldAddPadding)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(messages[position], shouldAddPadding)
        } else {
            Log.e("Chat Adapter","Why is this binding occurring?!!!")
        }
    }

    override fun getItemCount(): Int = messages.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    inner class SentMessageViewHolder(
        private val binding: ItemSentMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, shouldAddPadding: Boolean) {
            binding.message.text = message.text
            binding.timestamp.text = getTime(message.timestamp)

            val context = binding.root.context
            val normalPadding = context.resources.getDimensionPixelSize(com.tzere21.messengerapp.R.dimen.size_8)
            val extraPadding = context.resources.getDimensionPixelSize(com.tzere21.messengerapp.R.dimen.size_16)

            val topPadding = if (shouldAddPadding) normalPadding + extraPadding else normalPadding

            binding.root.setPadding(
                binding.root.paddingLeft,
                topPadding,
                binding.root.paddingRight,
                binding.root.paddingBottom
            )
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemReceivedMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, shouldAddPadding: Boolean) {
            binding.message.text = message.text
            binding.timestamp.text = getTime(message.timestamp)

            val context = binding.root.context
            val normalPadding = context.resources.getDimensionPixelSize(com.tzere21.messengerapp.R.dimen.size_8)
            val extraPadding = context.resources.getDimensionPixelSize(com.tzere21.messengerapp.R.dimen.size_16)

            val topPadding = if (shouldAddPadding) normalPadding + extraPadding else normalPadding

            binding.root.setPadding(
                binding.root.paddingLeft,
                topPadding,
                binding.root.paddingRight,
                binding.root.paddingBottom
            )
        }
    }

    private fun getTime(timestamp: Long) : String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}