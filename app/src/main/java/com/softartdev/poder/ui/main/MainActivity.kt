package com.softartdev.poder.ui.main

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import com.softartdev.poder.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_pokemon -> {
                message.setText(R.string.title_pokemon)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map -> {
                message.setText(R.string.title_map)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_downloads -> {
                message.setText(R.string.title_downloads)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }
}
