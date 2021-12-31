package com.codingblocksmodules.chitchat.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingblocksmodules.chitchat.*
import com.codingblocksmodules.chitchat.models.Inbox
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query

class InboxFragment : Fragment() {
    private lateinit var mAdapter:FirebaseRecyclerAdapter<Inbox, ChatViewHolder>
    private lateinit var recyclerView:RecyclerView

    private val mDatabase by lazy{
        FirebaseDatabase.getInstance()
    }

    private val auth by lazy{
        FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        setupAdapter()
        return view
    }

    private fun setupAdapter() {
        val baseQuery: Query = mDatabase.reference.child("chats").child(auth.uid!!)

        val options = FirebaseRecyclerOptions.Builder<Inbox>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setQuery(baseQuery, Inbox::class.java)
            .build()

        // Instantiate Firebase Recycler Adapter
        mAdapter = object:FirebaseRecyclerAdapter<Inbox, ChatViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
                return ChatViewHolder(layoutInflater.inflate(
                    R.layout.list_item,
                    parent,
                    false
                ))
            }

            override fun onBindViewHolder(holder: ChatViewHolder, position: Int, model: Inbox) {
                holder.bind(model){ name: String, photo: String, id: String ->
                    val intent = Intent(requireContext(), ChatActivity::class.java)
                    intent.putExtra(UID, id)
                    intent.putExtra(NAME, name)
                    intent.putExtra(IMAGE, photo)
                    startActivity(intent)
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()
        mAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        mAdapter.stopListening()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.apply{
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter
        }

    }
}
