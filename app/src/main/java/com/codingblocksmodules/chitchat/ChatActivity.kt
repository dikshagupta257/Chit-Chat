package com.codingblocksmodules.chitchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingblocksmodules.chitchat.adapters.ChatAdapter
import com.codingblocksmodules.chitchat.databinding.ActivityChatBinding
import com.codingblocksmodules.chitchat.models.*
import com.codingblocksmodules.chitchat.utils.KeyboardVisibilityUtil
import com.codingblocksmodules.chitchat.utils.isSameDayAs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val UID = "uid"
const val NAME = "name"
const val IMAGE = "image"
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    private val friendId by lazy{
        intent.getStringExtra(UID)
    }

    private val name by lazy{
        intent.getStringExtra(NAME)
    }

    private val image by lazy{
        intent.getStringExtra(IMAGE)
    }

    private val mCurrentUid by lazy{
        FirebaseAuth.getInstance().uid!!
    }

    private val db by lazy {
        FirebaseDatabase.getInstance()
    }

    private lateinit var currentUser: User
    private lateinit var chatAdapter:ChatAdapter
    private val messages = mutableListOf<ChatEvent>()
    private lateinit var keyboardVisibilityHelper: KeyboardVisibilityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmojiManager.install(GoogleEmojiProvider())
        binding = ActivityChatBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)

        keyboardVisibilityHelper = KeyboardVisibilityUtil(root){
            binding.msgRv.scrollToPosition(messages.size-1)
        }

        FirebaseFirestore.getInstance().collection("users").document(mCurrentUid).get()
            .addOnSuccessListener {
                currentUser = it.toObject(User::class.java)!!
            }

        chatAdapter = ChatAdapter(messages, mCurrentUid)

        binding.msgRv.apply{
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }
        binding.nameTv.text = name
        Picasso.get()
            .load(image)
            .error(R.drawable.defaultavatar)
            .placeholder(R.drawable.defaultavatar)
            .into(binding.userImgView)

        val emojiPopup = EmojiPopup.Builder.fromRootView(root).build(binding.msgEdtv)
        binding.smileBtn.setOnClickListener {
            emojiPopup.toggle()
        }

        //assignment-3 -> add code to implement the functionality of liking a message and swipe refresh functionality in chat activity
        binding.swipeToLoad.setOnRefreshListener {
            val workerScope = CoroutineScope(Dispatchers.Main)
            workerScope.launch {
                delay(2000)
                binding.swipeToLoad.isRefreshing = false
            }
        }

        binding.sendBtn.setOnClickListener {
            binding.msgEdtv.text?.let{
                if(it.isNotEmpty()){
                    sendMessage(it.toString())
                    it.clear()
                }
            }
        }
        listenToMessages(){msg, update ->
            if (update) {
                updateMessage(msg)
            } else {
                addMessages(msg)
            }

        }

        chatAdapter.highFiveClick = { id, status ->
            updateHighFive(id, status)
        }
        markAsRead()
    }

    private fun updateHighFive(id: String, status: Boolean){
        getMessages(friendId!!).child(id).updateChildren(mapOf("liked" to status))
    }

    private fun updateMessage(msg: Message) {
        val position = messages.indexOfFirst {
            when (it) {
                is Message -> it.msgId == msg.msgId
                else -> false
            }
        }
        messages[position] = msg

        chatAdapter.notifyItemChanged(position)
    }

    private fun listenToMessages(newMsg: (msg: Message, update: Boolean) -> Unit) {
        getMessages(friendId!!)
            .orderByKey()
            .addChildEventListener(object : ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val msg = snapshot.getValue(Message::class.java)
                    newMsg(msg!!, false)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val msg = snapshot.getValue(Message::class.java)!!
                    newMsg(msg, true)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    //TODO("Not yet implemented")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    //TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO("Not yet implemented")
                }

            })
    }

    private fun addMessages(msg: Message?) {
        val eventBefore = messages.lastOrNull()

        if((eventBefore!=null && !eventBefore.sentAt.isSameDayAs(msg!!.sentAt)) || eventBefore == null){
            messages.add(
                DateHeader(
                    msg!!.sentAt,
                    context = this
                )
            )
        }

        messages.add(msg!!)

        chatAdapter.notifyItemChanged(messages.size-1)
        binding.msgRv.scrollToPosition(messages.size-1)
    }

    private fun sendMessage(msg: String) {
        val id = getMessages(friendId!!).push().key //unique key
        checkNotNull(id){"ID cannot be null"}

        val msgMap = Message(msg, mCurrentUid, id)

        getMessages(friendId!!).child(id).setValue(msgMap)
            .addOnSuccessListener {
                Log.i("CHATS","completed")
            }.addOnFailureListener{
                Log.i("CHATS",it.localizedMessage)
            }

        updateLastMessage(msgMap)
    }

    private fun updateLastMessage(message: Message) {
        val inboxMap = Inbox(message.msg, friendId!!, name!!, image!!, count=0)

        getInbox(mCurrentUid, friendId!!).setValue(inboxMap)
            .addOnSuccessListener {
                getInbox(friendId!!, mCurrentUid).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val value = snapshot.getValue(Inbox::class.java)

                        inboxMap.apply {
                            from = message.senderId
                            name = currentUser.name
                            image = currentUser.thumbImage
                            count = 1
                        }

                        value?.let{
                            if(it.from == message.senderId){
                                inboxMap.count = value.count+1
                            }
                        }

                        getInbox(friendId!!, mCurrentUid).setValue(inboxMap)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        //TODO("Not yet implemented")
                    }

                })
            }
    }

    private fun markAsRead(){
        getInbox(mCurrentUid, friendId!!).child("count").setValue(0)
    }

    private fun getMessages(friendId:String) =
        db.reference.child("messages/${getId(friendId)}")

    private fun getInbox(toUser:String, fromUser:String) =
        db.reference.child("chats/$toUser/$fromUser")

    private fun getId(friendId: String):String{
        return if(friendId > mCurrentUid){
            mCurrentUid+friendId
        }else{
            friendId+mCurrentUid
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.viewTreeObserver
            .addOnGlobalLayoutListener(keyboardVisibilityHelper.visibilityListener)
    }


    override fun onPause() {
        super.onPause()
        binding.root.viewTreeObserver
            .removeOnGlobalLayoutListener(keyboardVisibilityHelper.visibilityListener)
    }
}