package com.codingblocksmodules.chitchat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codingblocksmodules.chitchat.R
import com.codingblocksmodules.chitchat.models.ChatEvent
import com.codingblocksmodules.chitchat.models.DateHeader
import com.codingblocksmodules.chitchat.models.Message
import com.codingblocksmodules.chitchat.utils.formatAsTime

class ChatAdapter(private val list:MutableList<ChatEvent>, private val mCurrentUid:String):
RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var highFiveClick: ((id: String, status: Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = {layout:Int ->        //higher order function for inflating the layout
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
        }

        return when(viewType){
            TEXT_MESSAGE_RECEIVED ->{
                MessageViewHolder(inflate(R.layout.list_item_chat_receive_message))
            }
            TEXT_MESSAGE_SENT -> {
                MessageViewHolder(inflate(R.layout.list_item_chat_sent_message))
            }
            DATE_HEADER ->{
                DateViewHolder(inflate(R.layout.list_item_date_header))
            }
            else ->{
                MessageViewHolder(inflate(R.layout.list_item_chat_receive_message))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int){
        when(val item = list[position]){
            is DateHeader -> {
                holder.itemView.findViewById<TextView>(R.id.textView).text = item.date
            }
            is Message -> {
                holder.itemView.apply{
                    findViewById<TextView>(R.id.content).text = item.msg
                    findViewById<TextView>(R.id.time).text = item.sentAt.formatAsTime()
                }
                val messageCardView = holder.itemView.findViewById<CardView>(R.id.messageCardView)
                val highFiveImg = holder.itemView.findViewById<ImageView>(R.id.highFiveImg)
                when (getItemViewType(position)) {
                    TEXT_MESSAGE_RECEIVED -> {
                        messageCardView.setOnClickListener(object :
                            DoubleClickListener() {
                            override fun onDoubleClick(v: View?) {
                                highFiveClick?.invoke(item.msgId, !item.liked)
                            }
                        })
                        highFiveImg.apply {
                            isVisible = position == itemCount - 1 || item.liked
                            isSelected = item.liked
                            setOnClickListener {
                                highFiveClick?.invoke(item.msgId, !isSelected)
                            }
                        }
                    }

                    TEXT_MESSAGE_SENT -> {
                        highFiveImg.apply {
                            isVisible = item.liked
                        }
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        return when(val event = list[position]){
            is Message -> {
                if(event.senderId == mCurrentUid){
                    TEXT_MESSAGE_SENT
                }else{
                    TEXT_MESSAGE_RECEIVED
                }
            }
            is DateHeader -> DATE_HEADER
            else -> UNSUPPORTED
        }
    }

    class DateViewHolder(view: View):RecyclerView.ViewHolder(view)
    class MessageViewHolder(view: View):RecyclerView.ViewHolder(view)

    companion object{
        private const val UNSUPPORTED = -1
        private const val TEXT_MESSAGE_RECEIVED = 0
        private const val TEXT_MESSAGE_SENT = 1
        private const val DATE_HEADER= 2
    }


}

//assignment-3 -> add code to implement the functionality of liking a message
abstract class DoubleClickListener : View.OnClickListener {
    private var lastClickTime: Long = 0
    override fun onClick(v: View?) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            onDoubleClick(v)
            lastClickTime = 0
        }
        lastClickTime = clickTime
    }

    //    abstract fun onSingleClick(v: View?)
    abstract fun onDoubleClick(v: View?)

    companion object {
        private const val DOUBLE_CLICK_TIME_DELTA: Long = 300 //milliseconds
    }
}