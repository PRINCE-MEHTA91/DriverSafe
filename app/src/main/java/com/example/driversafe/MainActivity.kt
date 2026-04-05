package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var logoutBtn: ImageView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        // ✅ Check user login
        if (mAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ✅ Logout Button
        logoutBtn = findViewById(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
      //start Button
        val destination = findViewById<EditText>(R.id.destinationEditText)
        val startBtn = findViewById<Button>(R.id.startButton)

        startBtn.setOnClickListener {

            val destinationText = destination.text.toString().trim()

            if (destinationText.isEmpty()) {
                destination.error = "Enter destination first"
                destination.requestFocus()
                return@setOnClickListener
            }

            val intent = Intent(this, JourneyMapActivity::class.java)
            intent.putExtra("to", destinationText)

            startActivity(intent)
        }




        // 🔥 Bottom Navigation Setup
        bottomNav = findViewById(R.id.bottomNav)

        // Default selected (Home)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    true // already here
                }

                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_documents -> {
                    startActivity(Intent(this, DocumentsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }
}