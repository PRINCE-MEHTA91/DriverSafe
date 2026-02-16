package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var forgetBtn: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        mAuth = FirebaseAuth.getInstance()

        email = findViewById(R.id.forgetpasswoad)
        forgetBtn = findViewById(R.id.forgetBtn)

        forgetBtn.setOnClickListener {

            val userEmail = email.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                email.error = "Enter valid Email"
                return@setOnClickListener
            }

            mAuth.sendPasswordResetEmail(userEmail)
                .addOnCompleteListener {
                    if (it.isSuccessful) {

                        Toast.makeText(this, "Reset link sent to Email", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
