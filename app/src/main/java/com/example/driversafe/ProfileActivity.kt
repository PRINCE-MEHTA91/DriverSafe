package com.example.driversafe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var storageRef: StorageReference

    private lateinit var profileImage: ImageView
    private var imageUri: Uri? = null

    // Modern way to handle Image Picking
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            profileImage.setImageURI(uri)
        }
    }
     
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        storageRef = FirebaseStorage.getInstance().reference

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = user.uid

        // Initialize Views
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etLicense = findViewById<EditText>(R.id.etLicense)
        val etSOS = findViewById<EditText>(R.id.etSOS)
        val spGender = findViewById<Spinner>(R.id.spGender)
        val spBlood = findViewById<Spinner>(R.id.spBlood)
        val vehicleContainer = findViewById<LinearLayout>(R.id.vehicleContainer)
        val btnAddVehicle = findViewById<Button>(R.id.btnAddVehicle)
        val btnSave = findViewById<Button>(R.id.btnSave)
        profileImage = findViewById(R.id.profileImage)

        // Set Email
        tvEmail.text = user.email ?: ""

        // Image Picker
        profileImage.setOnClickListener {
            getImage.launch("image/*")
        }

        // Setup Spinners
        val genderList = listOf("Select Gender", "Male", "Female", "Other")
        spGender.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderList)

        val bloodList =
            listOf("Select Blood Group", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
        spBlood.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodList)

        // Add vehicle button
        btnAddVehicle.setOnClickListener {
            addVehicleField(vehicleContainer, "")
        }

        // LOAD DATA FROM FIREBASE
        db.child("Users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                tvName.text = snapshot.child("name").value?.toString() ?: "User"
                etPhone.setText(snapshot.child("phone").value?.toString() ?: "")
                etLicense.setText(snapshot.child("license").value?.toString() ?: "")
                etSOS.setText(snapshot.child("sos").value?.toString() ?: "")

                // Set Spinner Selections
                val gender = snapshot.child("gender").value?.toString() ?: ""
                val blood = snapshot.child("bloodGroup").value?.toString() ?: ""
                spGender.setSelection(if (genderList.indexOf(gender) >= 0) genderList.indexOf(gender) else 0)
                spBlood.setSelection(if (bloodList.indexOf(blood) >= 0) bloodList.indexOf(blood) else 0)

                // Load Dynamic Vehicles
                vehicleContainer.removeAllViews()
                val vehicles = snapshot.child("vehicles").children
                for (v in vehicles) {
                    addVehicleField(vehicleContainer, v.value?.toString() ?: "")
                }
                if (vehicleContainer.childCount == 0) addVehicleField(vehicleContainer, "")

                // Load Profile Image
                val imageUrl = snapshot.child("profileImage").value?.toString()
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileActivity).load(imageUrl).into(profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // SAVE DATA
        btnSave.setOnClickListener {
            saveUserData(
                userId,
                tvName.text.toString(),
                tvEmail.text.toString(),
                etPhone,
                etLicense,
                etSOS,
                spGender,
                spBlood,
                vehicleContainer
            )
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }

                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }

                R.id.nav_documents -> {
                    startActivity(Intent(this, DocumentsActivity::class.java))
                    true
                }

                R.id.nav_profile -> true

                else -> false
            }
        }
    }

    private fun addVehicleField(container: LinearLayout, value: String) {
        val et = EditText(this)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        et.layoutParams = params
        et.hint = "Vehicle Number (e.g. ABC-1234)"
        et.setText(value)
        et.setBackgroundResource(android.R.drawable.edit_text) // Gives it a standard look
        container.addView(et)
    }

    private fun saveUserData(
        userId: String,
        name: String,
        email: String,
        etPhone: EditText,
        etLicense: EditText,
        etSOS: EditText,
        spGender: Spinner,
        spBlood: Spinner,
        container: LinearLayout
    ) {

        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            etPhone.error = "Required"
            return
        }

        val vehicleList = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is EditText) {
                val v = view.text.toString().trim()
                if (v.isNotEmpty()) vehicleList.add(v)
            }
        }

        val map = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "license" to etLicense.text.toString().trim(),
            "sos" to etSOS.text.toString().trim(),
            "gender" to spGender.selectedItem.toString(),
            "bloodGroup" to spBlood.selectedItem.toString(),
            "vehicles" to vehicleList
        )

        // 🔥 IMAGE UPLOAD + SAFE SAVE
        if (imageUri != null) {

            val fileRef = storageRef.child("profileImages/$userId.jpg")

            fileRef.putFile(imageUri!!)
                .addOnSuccessListener {

                    fileRef.downloadUrl.addOnSuccessListener { uri ->

                        map["profileImage"] = uri.toString()

                        db.child("Users").child(userId)
                            .updateChildren(map)   // 🔥 IMPORTANT CHANGE
                            .addOnSuccessListener {
                                Toast.makeText(this, "Details saved successfully ✅", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save data ❌", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed ❌", Toast.LENGTH_SHORT).show()
                }

        } else {

            db.child("Users").child(userId)
                .updateChildren(map)   // 🔥 IMPORTANT CHANGE
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Saved Permanently ✅", Toast.LENGTH_SHORT).show()
                }
        }
    }
}