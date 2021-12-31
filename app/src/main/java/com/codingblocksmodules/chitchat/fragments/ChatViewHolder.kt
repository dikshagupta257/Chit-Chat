package com.codingblocksmodules.chitchat.fragments

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codingblocksmodules.chitchat.R
import com.codingblocksmodules.chitchat.models.Inbox
import com.codingblocksmodules.chitchat.utils.formatAsListItem
import com.squareup.picasso.Picasso

class ChatViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
    fun bind(item: Inbox, onClick:(name:String, photo:String, id:String) -> Unit) = with(itemView){
        val counterTv = findViewById<TextView>(R.id.countTv)
        val timeTv = findViewById<TextView>(R.id.timeTv)
        val titleTv = findViewById<TextView>(R.id.titleTv)
        val subTitleTv = findViewById<TextView>(R.id.subTitleTv)
        val userImgView = findViewById<ImageView>(R.id.userImgView)

        counterTv.isVisible = item.count>0
        counterTv.text = item.count.toString()
        timeTv.text = item.time.formatAsListItem(context)
        titleTv.text = item.name
        subTitleTv.text = item.msg
        Picasso.get().load(item.image).placeholder(R.drawable.defaultavatar)
            .error(R.drawable.defaultavatar)
            .into(userImgView)

        setOnClickListener {
            onClick.invoke(item.name, item.image, item.from)
        }
    }
}