package com.codingblocksmodules.chitchat.models

import android.content.Context
import com.codingblocksmodules.chitchat.utils.formatAsHeader
import java.util.*

interface ChatEvent {
    val sentAt: Date
}

data class Message(
    val msg: String,
    val senderId: String,
    val msgId: String,
    val type: String = "TEXT",
    val status: Int = 1,
    val liked: Boolean = false,
    override val sentAt: Date = Date()
) : ChatEvent {
    constructor() : this("", "", "", "TEXT", 1, false, Date())
}

data class DateHeader(
    override val sentAt: Date = Date(),
    val context: Context
) : ChatEvent {
    val date: String = sentAt.formatAsHeader(context)
}