package com.example.driversafe

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        // 🔹 Firebase init
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        // 🔹 Views
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)

        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etLicense = findViewById<EditText>(R.id.etLicense)
        val etSOS = findViewById<EditText>(R.id.etSOS)

        val spGender = findViewById<Spinner>(R.id.spGender)
        val spBlood = findViewById<Spinner>(R.id.spBlood)

        val vehicleContainer = findViewById<LinearLayout>(R.id.vehicleContainer)
        val btnAddVehicle = findViewById<Button>(R.id.btnAddVehicle)
        val btnSave = findViewById<Button>(R.id.btnLogout)


        val userId = auth.currentUser?.uid

        // ✅ Gender Spinner
        val genderList = listOf("Male", "Female", "Trans", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderList)
        spGender.adapter = genderAdapter

        // ✅ Blood Group Spinner
        val bloodList = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
        val bloodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodList)
        spBlood.adapter = bloodAdapter

        // ✅ Add Vehicle Field
        btnAddVehicle.setOnClickListener {
            val et = EditText(this)
            et.hint = "Enter vehicle number"
            vehicleContainer.addView(et)
        }

        // 👉 One default vehicle field
        btnAddVehicle.performClick()

        // ✅ LOAD DATA FROM FIREBASE
        if (userId != null) {
            db.child("Users").child(userId).get().addOnSuccessListener {

                tvName.text = it.child("name").value.toString()
                tvEmail.text = it.child("email").value.toString()

                etPhone.setText(it.child("phone").value.toString())
                etLicense.setText(it.child("license").value.toString())
                etSOS.setText(it.child("sos").value.toString())

                // Spinner set
                val gender = it.child("gender").value.toString()
                val blood = it.child("bloodGroup").value.toString()

                spGender.setSelection(genderList.indexOf(gender))
                spBlood.setSelection(bloodList.indexOf(blood))

                // Vehicles load
                val vehicles = it.child("vehicles").children
                vehicleContainer.removeAllViews()

                for (v in vehicles) {
                    val et = EditText(this)
                    et.setText(v.value.toString())
                    vehicleContainer.addView(et)
                }
            }
        }

        // ✅ SAVE DATA
        btnSave.setOnClickListener {

            val phone = etPhone.text.toString()
            val license = etLicense.text.toString()
            val sos = etSOS.text.toString()
            val gender = spGender.selectedItem.toString()
            val blood = spBlood.selectedItem.toString()

            val vehicleList = ArrayList<String>()
            for (i in 0 until vehicleContainer.childCount) {
                val et = vehicleContainer.getChildAt(i) as EditText
                vehicleList.add(et.text.toString())
            }

            val map = HashMap<String, Any>()
            map["phone"] = phone
            map["license"] = license
            map["sos"] = sos
            map["gender"] = gender
            map["bloodGroup"] = blood
            map["vehicles"] = vehicleList

            if (userId != null) {
                db.child("Users").child(userId).updateChildren(map)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        // ✅ Bottom Navigation connect (IMPORTANT: bottomNav id use karo)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)


        // 🔵 Current selected item
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
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
                    true // already on profile
                }

                else -> false
            }
        }
    }
}