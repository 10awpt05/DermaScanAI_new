package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityUserInboxBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserInboxActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInboxBinding
    private val dermaList = ArrayList<Pair<DermaInfo, String>>() // Derma + LastMessage
    private lateinit var adapter: DermaListAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val dbUrl = "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = DermaListAdapter(dermaList) { derma ->
            val intent = Intent(this, MessageMe::class.java)
            intent.putExtra("receiverId", derma.uid)
            intent.putExtra("name", derma.name)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadChatList()
    }

//    private fun loadChatList() {
//        val chatRef = FirebaseDatabase.getInstance(dbUrl)
//            .getReference("userChats").child(currentUserId)
//
//        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                for (chatPartnerSnapshot in snapshot.children) {
//                    val partnerId = chatPartnerSnapshot.key ?: continue
//                    loadDermaInfoWithLastMessage(partnerId)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {}
//        })
//    }

    private fun loadChatList() {
        val messagesRef = FirebaseDatabase.getInstance(dbUrl).getReference("messages")

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dermaList.clear()  // Clear the list to repopulate

                val tempMap = mutableMapOf<String, Pair<DermaInfo, Long>>()  // uid -> (DermaInfo, latestTimestamp)

                val messageMap = mutableMapOf<String, String>() // uid -> last message

                for (msgSnap in snapshot.children) {
                    val senderId = msgSnap.child("senderId").getValue(String::class.java) ?: continue
                    val receiverId = msgSnap.child("receiverId").getValue(String::class.java) ?: continue
                    val messageText = msgSnap.child("text").getValue(String::class.java) ?: ""
                    val timestamp = msgSnap.child("timestamp").getValue(Long::class.java) ?: 0

                    val chatPartnerId = when (currentUserId) {
                        senderId -> receiverId
                        receiverId -> senderId
                        else -> continue
                    }

                    if (!tempMap.containsKey(chatPartnerId) || timestamp > (tempMap[chatPartnerId]?.second ?: 0)) {
                        // Fetch derma info and update map
                        val dermaRef = FirebaseDatabase.getInstance(dbUrl).getReference("dermaInfo").child(chatPartnerId)
                        dermaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val derma = snapshot.getValue(DermaInfo::class.java)
                                if (derma != null) {
                                    derma.uid = chatPartnerId
                                    tempMap[chatPartnerId] = Pair(derma, timestamp)
                                    messageMap[chatPartnerId] = messageText

                                    // Once all data is added, refresh list
                                    if (tempMap.size == messageMap.size) {
                                        updateRecyclerView(tempMap, messageMap)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateRecyclerView(
        tempMap: Map<String, Pair<DermaInfo, Long>>,
        messageMap: Map<String, String>
    ) {
        dermaList.clear()

        // Sort by latest timestamp descending
        val sortedList = tempMap.entries.sortedByDescending { it.value.second }

        for (entry in sortedList) {
            val derma = entry.value.first
            val lastMessage = messageMap[entry.key] ?: "No message"
            dermaList.add(Pair(derma, lastMessage))
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadDermaInfoWithLastMessage(partnerId: String) {
        val dermaRef = FirebaseDatabase.getInstance(dbUrl)
            .getReference("dermaInfo").child(partnerId)

        dermaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val derma = snapshot.getValue(DermaInfo::class.java)
                if (derma != null) {
                    derma.uid = partnerId
                    loadLastMessageForDerma(derma)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun loadLastMessageForDerma(derma: DermaInfo) {
        val messagesRef = FirebaseDatabase.getInstance(dbUrl).getReference("messages")

        messagesRef.orderByChild("timestamp").limitToLast(50) // Keep query efficient
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var lastMessage: String? = null
                    var latestTimestamp = 0L

                    for (msgSnap in snapshot.children) {
                        val sender = msgSnap.child("senderId").getValue(String::class.java)
                        val receiver = msgSnap.child("receiverId").getValue(String::class.java)
                        val messageText = msgSnap.child("text").getValue(String::class.java)
                        val timestamp = msgSnap.child("timestamp").getValue(Long::class.java) ?: 0

                        val isRelevant = (sender == currentUserId && receiver == derma.uid) ||
                                (receiver == currentUserId && sender == derma.uid)

                        if (isRelevant && timestamp > latestTimestamp) {
                            lastMessage = messageText
                            latestTimestamp = timestamp
                        }
                    }

                    val displayMessage = lastMessage ?: "No messages yet"

                    if (!dermaList.any { it.first.uid == derma.uid }) {
                        dermaList.add(Pair(derma, displayMessage))
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }



    private fun loadDermaInfoAndLastMessage(uid: String) {
        val dermaRef = FirebaseDatabase.getInstance(dbUrl)
            .getReference("dermaInfo").child(uid)

        dermaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val derma = snapshot.getValue(DermaInfo::class.java)
                if (derma != null) {
                    derma.uid = uid
                    loadLastMessage(derma)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLastMessage(derma: DermaInfo) {
        val dermaUid = derma.uid ?: return  // Exit if UID is null

        val chatId = if (currentUserId < dermaUid) {
            "${currentUserId}_$dermaUid"
        } else {
            "${dermaUid}_$currentUserId"
        }

        val messagesRef = FirebaseDatabase.getInstance(dbUrl).getReference("messages").child(chatId)

        messagesRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var lastMsg = "No messages yet"
                for (msgSnap in snapshot.children) {
                    lastMsg = msgSnap.child("messages").getValue(String::class.java) ?: lastMsg
                }

                if (!dermaList.any { it.first.uid == dermaUid }) {
                    dermaList.add(Pair(derma, lastMsg))
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}
