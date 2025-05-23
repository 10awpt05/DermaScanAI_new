package com.example.dermascanai

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemResponseViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

interface OnCommentReplyListener {
    fun onReply(parentCommentId: String)
}

class ReplyAdapter(private val replies: List<Comment>) :
    RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder>() {

    inner class ReplyViewHolder(val binding: ItemResponseViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val binding = ItemResponseViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReplyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        holder.binding.textView33.text = reply.comment
        holder.binding.linearLayout.visibility = View.GONE // hide reply/heart bar for replies
        holder.binding.recyclerViewComment.visibility = View.GONE // don't nest replies again
        holder.binding.linearLayout2.visibility = View.GONE
        reply.userProfileImageBase64?.let {
            try {
                val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.binding.profile.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }

        reply.userId?.let { uid ->
            FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").reference.child("userInfo").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        holder.binding.name.text =
                            snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        holder.binding.name.text = "Unknown"
                    }
                })
        }
    }

    override fun getItemCount(): Int = replies.size
}
