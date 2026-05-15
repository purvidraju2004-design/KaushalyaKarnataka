package com.example.kaushalyakarnataka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class WorkerAdapter(private var workerList: List<WorkerProfile>) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgWorkerWork: ImageView = itemView.findViewById(R.id.imgWorkerWork)
        val tvWorkerName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvWorkerCategory: TextView = itemView.findViewById(R.id.tvWorkerCategory)
        val tvWorkerService: TextView = itemView.findViewById(R.id.tvWorkerService)
        val tvWorkerDescription: TextView = itemView.findViewById(R.id.tvWorkerDescription)
        val tvWorkerPrice: TextView = itemView.findViewById(R.id.tvWorkerPrice)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val btnHireMe: Button = itemView.findViewById(R.id.btnHireMe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.worker_item, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val currentWorker = workerList[position]

        holder.tvWorkerName.text = currentWorker.name
        holder.tvWorkerCategory.text = currentWorker.category
        holder.tvWorkerService.text = currentWorker.service
        holder.tvWorkerDescription.text = currentWorker.description
        holder.tvWorkerPrice.text = "Starting at: ₹${currentWorker.price}"

        if (currentWorker.rating == 0.0f) {
            holder.tvRating.text = "★ New"
        } else {
            holder.tvRating.text = "★ ${currentWorker.rating}"
        }

        Glide.with(holder.itemView.context).load(currentWorker.imageUrl).centerCrop().into(holder.imgWorkerWork)

        // --- HIRE ME NOTIFICATION ---
        holder.btnHireMe.setOnClickListener {
            val context = holder.itemView.context
            val db = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // ANTI-SPAM LOCK: Prevent the worker from hiring themselves
                if (currentUser.uid == currentWorker.creatorId) {
                    Toast.makeText(context, "You cannot hire yourself!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val notificationData = hashMapOf(
                    "toUserId" to currentWorker.creatorId, // FIXED: Sending to the actual creator!
                    "fromUserId" to currentUser.uid,
                    "fromUserName" to currentUser.displayName,
                    "message" to "${currentUser.displayName ?: "A customer"} wants to hire you for ${currentWorker.service}!",
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("notifications").add(notificationData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Hire request sent to ${currentWorker.name}!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "You must be logged in to hire someone!", Toast.LENGTH_SHORT).show()
            }
        }

        // --- RATING SYSTEM ---
        holder.tvRating.setOnClickListener {
            val context = holder.itemView.context
            val currentUser = FirebaseAuth.getInstance().currentUser

            // SECURITY LOCK: Workers cannot rate their own profile
            if (currentUser != null && currentUser.uid == currentWorker.creatorId) {
                Toast.makeText(context, "You cannot rate your own profile!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ratingBar = android.widget.RatingBar(context)
            ratingBar.numStars = 5
            ratingBar.stepSize = 0.5f
            ratingBar.rating = currentWorker.rating

            val layout = android.widget.LinearLayout(context)
            layout.gravity = android.view.Gravity.CENTER
            layout.setPadding(0, 50, 0, 0)
            layout.addView(ratingBar)

            android.app.AlertDialog.Builder(context)
                .setTitle("Rate ${currentWorker.name}")
                .setView(layout)
                .setPositiveButton("Submit") { _, _ ->
                    val newRating = ratingBar.rating
                    FirebaseFirestore.getInstance().collection("Workers").document(currentWorker.id)
                        .update("rating", newRating)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                            currentWorker.rating = newRating
                            notifyItemChanged(position)
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- EDIT PROFILE SYSTEM ---
        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            val currentUser = FirebaseAuth.getInstance().currentUser

            // SECURITY LOCK: Only the actual creator can open the edit menu!
            if (currentUser == null || currentUser.uid != currentWorker.creatorId) {
                Toast.makeText(context, "Locked: You can only edit your own profile!", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }

            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            val editService = EditText(context).apply { hint = "Service Name"; setText(currentWorker.service) }
            val editPrice = EditText(context).apply { hint = "Price"; setText(currentWorker.price) }
            val editDescription = EditText(context).apply { hint = "Description"; setText(currentWorker.description) }

            layout.addView(editService)
            layout.addView(editPrice)
            layout.addView(editDescription)

            android.app.AlertDialog.Builder(context)
                .setTitle("Edit Your Profile")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val updates = mapOf(
                        "service" to editService.text.toString(),
                        "price" to editPrice.text.toString(),
                        "description" to editDescription.text.toString()
                    )

                    FirebaseFirestore.getInstance().collection("Workers").document(currentWorker.id)
                        .update(updates)
                        .addOnSuccessListener {
                            currentWorker.service = editService.text.toString()
                            currentWorker.price = editPrice.text.toString()
                            currentWorker.description = editDescription.text.toString()
                            notifyItemChanged(position)
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }
    }

    override fun getItemCount() = workerList.size

    fun updateList(newList: List<WorkerProfile>) {
        workerList = newList
        notifyDataSetChanged()
    }
}