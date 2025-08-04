package com.example.dermascanai

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemMessageReceiverBinding
import com.example.dermascanai.databinding.ItemMessageSenderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MessageAdapter(
    private val context: Context,
    private val messages: List<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENDER = 1
        private const val VIEW_TYPE_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) VIEW_TYPE_SENDER else VIEW_TYPE_RECEIVER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENDER) {
            val binding = ItemMessageSenderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SenderViewHolder(binding)
        } else {
            val binding = ItemMessageReceiverBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceiverViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timestamp = message.filePath

        if (holder is SenderViewHolder) {
            holder.binding.messageText.text = message.text

            if (!timestamp.isNullOrEmpty()) {
                holder.binding.attachedFile.visibility = View.VISIBLE
                holder.binding.attachedFile.setOnClickListener {
                    Toast.makeText(context, "Clicked!", Toast.LENGTH_SHORT).show()
                    fetchAndShowScan(context, message.senderId, timestamp)
                }
            } else {
                holder.binding.attachedFile.visibility = View.GONE
            }

        } else if (holder is ReceiverViewHolder) {
            holder.binding.messageText.text = message.text

            if (!timestamp.isNullOrEmpty()) {
                holder.binding.attachedFile.visibility = View.VISIBLE
                holder.binding.attachedFile.setOnClickListener {
                    Toast.makeText(context, "Clicked!", Toast.LENGTH_SHORT).show()
                    fetchAndShowScan(context, message.senderId, timestamp)
                }
            } else {
                holder.binding.attachedFile.visibility = View.GONE
            }
        }
    }

    private fun fetchAndShowScan(context: Context, userId: String, timestamp: String) {
        val fullPath = "scanResults/$userId/$timestamp"
        Log.d("MessageAdapter", "Fetching scan from: $fullPath")

        val ref = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference(fullPath)

        ref.get().addOnSuccessListener { snapshot ->
            Log.d("MessageAdapter", "Data snapshot exists: ${snapshot.exists()}")
            if (!snapshot.exists()) {
                Log.e("MessageAdapter", "No data found at: $fullPath")
                Toast.makeText(context, "No scan data found", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val condition = snapshot.child("condition").value?.toString() ?: "Unknown"
            val imageBase64 = snapshot.child("imageBase64").value?.toString() ?: ""
            val remedy = snapshot.child("remedy").value?.toString() ?: "No remedy provided"

            if (imageBase64.isNotEmpty()) {
                showScanPopup(context, condition, imageBase64, remedy)
            } else {
                Log.e("MessageAdapter", "Image base64 was empty")
                Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener { error ->
            Log.e("MessageAdapter", "Failed to load scan data", error)
            Toast.makeText(context, "Failed to load scan data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showScanPopup(context: Context, condition: String, imageBase64: String, remedy: String) {
        try {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_image_popup)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val imageView = dialog.findViewById<ImageView>(R.id.popupImageView)
            val conditionText = dialog.findViewById<TextView>(R.id.conditionText)
            val remedyText = dialog.findViewById<TextView>(R.id.remedyText)

            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            imageView.setImageBitmap(bitmap)
            conditionText.text = "Condition: $condition"
            remedyText.text = "Remedy: $remedy"

            dialog.show()
            Log.d("MessageAdapter", "Dialog shown successfully")

        } catch (e: Exception) {
            Log.e("MessageAdapter", "Error showing popup", e)
            Toast.makeText(context, "Error displaying image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class SenderViewHolder(val binding: ItemMessageSenderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ReceiverViewHolder(val binding: ItemMessageReceiverBinding) : RecyclerView.ViewHolder(binding.root)
}
