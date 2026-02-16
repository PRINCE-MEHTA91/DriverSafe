package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var welcomeText: TextView
    private lateinit var logoutBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()


        if (mAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }


        welcomeText = findViewById(R.id.welcomeText)
        logoutBtn = findViewById(R.id.logoutBtn)

        val userEmail = mAuth.currentUser?.email
        val user = mAuth.currentUser
        val userName = user?.displayName
        welcomeText.text = "Welcome $userName\n$userEmail"

        logoutBtn.setOnClickListener {
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
