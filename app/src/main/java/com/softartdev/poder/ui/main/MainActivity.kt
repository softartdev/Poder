package com.softartdev.poder.ui.main

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.softartdev.poder.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)

        main_bottom_navigation_view.setOnNavigationItemSelectedListener(MainNavigation(supportFragmentManager))
        if (savedInstanceState == null) {
            main_bottom_navigation_view.selectedItemId = R.id.navigation_map
        }
    }

}
