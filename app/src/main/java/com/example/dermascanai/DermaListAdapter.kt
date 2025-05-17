package com.example.dermascanai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemUserChatBinding

class DermaListAdapter(
    private val dermas: List<Pair<DermaInfo, String>>,
    private val onClick: (DermaInfo) -> Unit
) : RecyclerView.Adapter<DermaListAdapter.DermaViewHolder>() {

    inner class DermaViewHolder(private val binding: ItemUserChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pair: Pair<DermaInfo, String>) {
            val derma = pair.first
            val lastMessage = pair.second

            binding.userName.text = derma.name ?: "Derma"
            binding.lastMessage.text = lastMessage
            binding.root.setOnClickListener { onClick(derma) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DermaViewHolder {
        val binding = ItemUserChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DermaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DermaViewHolder, position: Int) {
        holder.bind(dermas[position])
    }

    override fun getItemCount(): Int = dermas.size
}

