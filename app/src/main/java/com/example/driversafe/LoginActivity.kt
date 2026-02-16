package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var forgotTxt: TextView
    private lateinit var signupTxt: TextView
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        // 🔥 Auto Login
        if (mAuth.currentUser != null && mAuth.currentUser!!.isEmailVerified) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_login)

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        loginBtn = findViewById(R.id.loginBtn)
        forgotTxt = findViewById(R.id.forgotTxt)
        signupTxt = findViewById(R.id.signupTxt)

        loginBtn.setOnClickListener { loginUser() }

        forgotTxt.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signupTxt.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loginUser() {

        val userEmail = email.text.toString().trim()
        val userPassword = password.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.error = "Enter valid Email"
            return
        }

        if (userPassword.isEmpty()) {
            password.error = "Enter Password"
            return
        }

        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {

                    val user = mAuth.currentUser

                    user?.reload()?.addOnCompleteListener {

                        if (user.isEmailVerified) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Verify your Email first!", Toast.LENGTH_LONG).show()
                        }
                    }

                } else {
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }

    }
}
