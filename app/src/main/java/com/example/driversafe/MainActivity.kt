package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var logoutBtn:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        // check user login
        if (mAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        logoutBtn = findViewById(R.id.logoutBtn)

        logoutBtn.setOnClickListener {

            mAuth.signOut()

            startActivity(Intent(this, LoginActivity::class.java))

            finish()
        }
    }
}