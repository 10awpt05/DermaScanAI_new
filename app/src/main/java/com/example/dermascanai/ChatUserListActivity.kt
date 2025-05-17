package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityChatUserListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatUserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatUserListBinding
    private lateinit var database: DatabaseReference
    private lateinit var userList: ArrayList<UserInfo>
    private lateinit var adapter: UserListAdapter
    private val dermaId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userList = ArrayList()
        adapter = UserListAdapter(userList) { user ->
            // Pass the UID as "receiverId" for the chat
            val intent = Intent(this, MessageMe::class.java)
            intent.putExtra("receiverId", user.uid)
            intent.putExtra("name", user.name)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadChatUsers()
//        loadDermasWhoMessagedUser()
    }

    private fun loadChatUsers() {
        val userChatsRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("userChats")
            .child(dermaId)

        userChatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    val userInfoRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("userInfo").child(userId)

                    userInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(data: DataSnapshot) {
                            val user = data.getValue(UserInfo::class.java)
                            if (user != null) {
                                user.uid = userId
                                userList.add(user)
                                adapter.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun loadDermasWhoMessagedUser() {
        val currentUserId = dermaId
        val messagesRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("userChats")
            .child(currentUserId)

        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senderIds = HashSet<String>()

                for (chatSnapshot in snapshot.children) {
                    for (messageSnapshot in chatSnapshot.children) {
                        val senderId = messageSnapshot.child("senderId").getValue(String::class.java)
                        val receiverId = messageSnapshot.child("receiverId").getValue(String::class.java)

                        if (receiverId == currentUserId && senderId != null) {
                            senderIds.add(senderId)
                        }
                    }
                }

                for (senderId in senderIds) {
                    val userInfoRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("userInfo")
                        .child(senderId)

                    userInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(UserInfo::class.java)
                            if (user != null) {
                                user.uid = senderId
                                if (!userList.any { it.uid == user.uid }) {
                                    userList.add(user)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}