package com.example.detectorRuSign

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (this !is MainActivity) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                }
                R.id.navigation_signs -> {
                    val intent = Intent(this, SignsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_rules -> {
                    val intent = Intent(this, RulesActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_camera -> {
                    val intent = Intent(this, DetectionActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            true
        }
    }
}
