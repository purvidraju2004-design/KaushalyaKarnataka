package com.example.kaushalyakarnataka // Keep your exact package name!

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // 1. Declare our UI and Data variables
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var adapter: WorkerAdapter
    private lateinit var workerList: ArrayList<WorkerProfile>
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Link code to XML layouts
        recyclerView = findViewById(R.id.recyclerViewWorkers)
        searchBar = findViewById(R.id.searchBar)
        val fabAddWorker = findViewById<FloatingActionButton>(R.id.fabAddWorker)
        val btnLogout = findViewById<Button>(R.id.btnLogout) // --- NEW: Find the Logout button ---

        // Setup the list format (scrolling top to bottom)
        recyclerView.layoutManager = LinearLayoutManager(this)
        workerList = arrayListOf()
        adapter = WorkerAdapter(workerList)
        recyclerView.adapter = adapter

        // 3. Keep the '+' button working
        fabAddWorker.setOnClickListener {
            val intent = Intent(this, AddWorkerActivity::class.java)
            startActivity(intent)
        }

        // --- NEW: LOGOUT BUTTON LOGIC ---
        btnLogout.setOnClickListener {
            // Tell Firebase to securely log this user out
            FirebaseAuth.getInstance().signOut()

            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

            // Send them back to the Login Screen
            val intent = Intent(this, LoginActivity::class.java)

            // This clears the app history so they can't just press the phone's "Back" button to get back into the dashboard!
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
        // --------------------------------

        // 4. Load the data from Firebase
        fetchWorkersFromCloud()

        // 5. Make the Search Bar work (Success Criteria: Filter by Category)
        setupSearchBar()

        // --- REAL-TIME NOTIFICATION LISTENER ---
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Listen to the "notifications" database folder, but ONLY for messages meant for me!
            db.collection("notifications")
                .whereEqualTo("toUserId", currentUser.uid)
                .addSnapshotListener { snapshots, e ->

                    // If the database connection fails, just stop.
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    // Look through any new changes in the database
                    for (change in snapshots!!.documentChanges) {

                        // We only care if a BRAND NEW notification was added
                        if (change.type == DocumentChange.Type.ADDED) {

                            val message = change.document.getString("message") ?: "Someone wants to hire you!"

                            // Trigger a giant pop-up alert on the screen instantly!
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("🚨 New Job Request!")
                                .setMessage(message)
                                .setPositiveButton("Awesome!") { dialog, _ ->
                                    dialog.dismiss()

                                    // Delete the notification from the database after reading it
                                    // so it doesn't pop up again the next time they open the app!
                                    change.document.reference.delete()
                                }
                                .show()
                        }
                    }
                }
        }
        // ----------------------------------------
    }

    private fun fetchWorkersFromCloud() {
        // Look inside the "Workers" collection in our database
        db.collection("Workers").get()
            .addOnSuccessListener { result ->
                workerList.clear() // Clear the list so we don't get duplicates

                for (document in result) {
                    // Convert the raw database text back into our WorkerProfile object
                    val worker = document.toObject(WorkerProfile::class.java)
                    workerList.add(worker)
                }

                // Tell the adapter that new data has arrived so it refreshes the screen
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load workers.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearchBar() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Get whatever the user typed in the search bar
                val searchText = s.toString().lowercase()

                // Create a temporary list to hold the matching workers
                val filteredList = ArrayList<WorkerProfile>()

                for (worker in workerList) {
                    // If the worker's category contains the typed text, add them to the filter list
                    if (worker.category.lowercase().contains(searchText)) {
                        filteredList.add(worker)
                    }
                }

                // Update the screen with only the filtered workers
                adapter.updateList(filteredList)
            }
        })
    }

    // This ensures that when you add a new worker and press 'back', the list refreshes automatically
    override fun onResume() {
        super.onResume()
        fetchWorkersFromCloud()
    }
}