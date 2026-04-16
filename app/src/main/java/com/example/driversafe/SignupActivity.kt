package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignupActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var signupBtn: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mAuth = FirebaseAuth.getInstance()

        fullName = findViewById(R.id.fullName)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        signupBtn = findViewById(R.id.signupBtn)

        signupBtn.setOnClickListener { registerUser() }
    }

    private fun registerUser() {

        val name = fullName.text.toString().trim()
        val userEmail = email.text.toString().trim()
        val userPassword = password.text.toString().trim()
        val confirmPass = confirmPassword.text.toString().trim()

        // ✅ Validation
        if (name.isEmpty()) {
            fullName.error = "Enter Name"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.error = "Enter valid Email"
            return
        }
        if (userPassword.length < 6) {
            password.error = "Password must be 6+ characters"
            return
        }
        if (userPassword != confirmPass) {
            confirmPassword.error = "Password not match"
            return
        }

        // 🔥 CREATE USER FIRST
        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {

                    val user = mAuth.currentUser

                    // 🔥 NOW UPDATE NAME (IMPORTANT)
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {

                        user.reload()   // 🔥 IMPORTANT

                        val updatedUser = FirebaseAuth.getInstance().currentUser

                        Toast.makeText(this, "Name: ${updatedUser?.displayName}", Toast.LENGTH_LONG).show()

                        updatedUser?.sendEmailVerification()

                        Toast.makeText(this, "Account Created! Verify Email", Toast.LENGTH_LONG).show()

                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }

                } else {
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
}