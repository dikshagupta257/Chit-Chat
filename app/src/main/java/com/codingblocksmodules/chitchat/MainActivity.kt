package com.codingblocksmodules.chitchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.codingblocksmodules.chitchat.adapters.ScreenSliderAdapter
import com.codingblocksmodules.chitchat.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.viewPager.adapter = ScreenSliderAdapter(this)
        TabLayoutMediator(binding.tabs,
            binding.viewPager
        ) { tab: TabLayout.Tab, pos: Int ->
            when (pos) {
                0 -> tab.text = "CHATS"
                1 -> tab.text = "PEOPLE"
            }
        }.attach()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.logout -> LogOut()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun LogOut() {
        FirebaseAuth.getInstance().signOut()
        val i = Intent(this, LoginActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
}