package com.example.dermascanai

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dermascanai.databinding.ItemUserChatBinding
import java.io.ByteArrayInputStream

class UserListAdapter(
    private val users: List<UserInfo>,
    private val onUserClick: (UserInfo) -> Unit
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserInfo) {
            binding.userName.text = user.name ?: "Unknown"
            binding.root.setOnClickListener { onUserClick(user) }
            // You can load profile image here if needed using Glide or Picasso
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}