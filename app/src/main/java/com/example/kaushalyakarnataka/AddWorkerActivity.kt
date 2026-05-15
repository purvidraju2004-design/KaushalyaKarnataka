package com.example.kaushalyakarnataka // Keep your exact package name here!

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddWorkerActivity : AppCompatActivity() {

    // 1. Declare our UI Elements
    private lateinit var ivSelectedImage: ImageView
    private lateinit var etWorkerName: EditText
    private lateinit var etCategory: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSaveProfile: Button

    // 2. Declare Firebase variables (REMOVED FirebaseStorage entirely!)
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()

    // 3. This is the modern Android way to open the phone's gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            ivSelectedImage.setImageURI(uri) // Show the chosen picture on screen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_worker)

        // 4. Link our code variables to the actual XML layout IDs
        ivSelectedImage = findViewById(R.id.ivSelectedImage)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        etWorkerName = findViewById(R.id.etWorkerName)
        etCategory = findViewById(R.id.etCategory)
        etPrice = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // 5. Open Gallery when "Select Photo" is clicked (Still works visually!)
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 6. Start the upload process when "Save" is clicked
        btnSaveProfile.setOnClickListener {
            uploadDataToFirebase()
        }
    }

    private fun uploadDataToFirebase() {
        // Grab what the user typed
        val name = etWorkerName.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val price = etPrice.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Check if anything is empty (REMOVED imageUri check so it doesn't block you)
        if (name.isEmpty() || category.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Please fill all text fields!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Uploading... Please wait", Toast.LENGTH_LONG).show()
        btnSaveProfile.isEnabled = false // Disable button so they don't click it twice

        // THE BYPASS: Generate a professional profile picture automatically using their name
        val defaultImageUrl = "https://ui-avatars.com/api/?name=${name}&background=random&size=256"

        // Save straight to Firestore Database, bypassing the Storage errors entirely
        saveTextDataToFirestore(name, category, price, description, defaultImageUrl)
    }

    private fun saveTextDataToFirestore(name: String, category: String, price: String, description: String, imageUrl: String) {
        // Generate a unique ID for this specific worker document
        val workerId = db.collection("Workers").document().id

        // --- NEW: Grab the current logged-in user's Google ID! ---
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // FIXED: Using Kotlin's "Named Arguments" to perfectly map the data to the correct boxes!
        val worker = WorkerProfile(
            id = workerId,
            name = name,
            category = category,
            service = "", // Adding our new service variable as a blank default
            price = price,
            description = description,
            imageUrl = imageUrl,
            rating = 0.0f,
            creatorId = currentUserId // --- NEW: THE SECRET STAMP! ---
        )

        // Save it to the "Workers" collection
        db.collection("Workers").document(workerId).set(worker)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Live! Successfully saved.", Toast.LENGTH_SHORT).show()
                finish() // This closes the screen and takes you back to the Dashboard
            }
            .addOnFailureListener {
                Toast.makeText(this, "Database Error. Could not save profile.", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
            }
    }
}