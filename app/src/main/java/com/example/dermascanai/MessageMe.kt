package com.example.dermascanai

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityMessageMeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessageMe : AppCompatActivity() {

    private lateinit var binding: ActivityMessageMeBinding
    private lateinit var database: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = ArrayList<Message>()
    private var receiverId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("messages")

        // Get receiver ID from intent
        receiverId = intent.getStringExtra("receiverId")

        binding.backBTN.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadMessages()

        binding.send.setOnClickListener {
            val messageText = binding.messageText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.messageText.setText("")
            }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList)
        binding.recycleView.layoutManager = LinearLayoutManager(this)
        binding.recycleView.adapter = messageAdapter
    }

    private fun loadMessages() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnap in snapshot.children) {
                    val message = messageSnap.getValue(Message::class.java)
                    if (message != null) {
                        // Only show messages between these two users
                        if ((message.senderId == FirebaseAuth.getInstance().currentUser?.uid && message.receiverId == receiverId) ||
                            (message.receiverId == FirebaseAuth.getInstance().currentUser?.uid && message.senderId == receiverId)
                        ) {
                            messageList.add(message)
                        }
                    }
                }
                messageAdapter.notifyDataSetChanged()
                binding.recycleView.scrollToPosition(messageList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessageMe, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage(text: String) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val receiverId = this.receiverId ?: return
        val messageId = database.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(messageId, senderId, receiverId, text, timestamp)
        database.child(messageId).setValue(message)

        // Save chat relationship
        val chatRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("userChats")
        chatRef.child(senderId).child(receiverId).setValue(true)
        chatRef.child(receiverId).child(senderId).setValue(true)

        saveNotification(senderId, receiverId)
    }

    private fun saveNotification(fromUserId: String, toUserId: String) {
        val dbRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        // Try userInfo first
        dbRef.child("userInfo").child(fromUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                if (userSnapshot.exists()) {
                    val fullName = userSnapshot.child("name").getValue(String::class.java) ?: "Someone"
                    pushNotification(fullName, fromUserId, toUserId)
                } else {
                    // If not a user, check dermaInfo
                    dbRef.child("dermaInfo").child(fromUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dermaSnapshot: DataSnapshot) {
                            val fullName = dermaSnapshot.child("name").getValue(String::class.java) ?: "Someone"
                            pushNotification(fullName, fromUserId, toUserId)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MessageMe, "Error fetching derma info", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessageMe, "Error fetching user info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun pushNotification(fullName: String, fromUserId: String, toUserId: String) {
        val notificationRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("notifications").child(toUserId)
        val notificationId = notificationRef.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val notificationData = mapOf(
            "notificationId" to notificationId,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "message" to "$fullName has sent you a message.",
            "postId" to "",
            "type" to "message",
            "isRead" to false,
            "timestamp" to timestamp
        )

        notificationRef.child(notificationId).setValue(notificationData)
    }
}
