package com.example.kaushalyakarnataka // Make sure this matches!

data class WorkerProfile(
    var id: String = "",
    val name: String = "",
    val category: String = "",
    var service: String = "",
    var price: String = "",
    var description: String = "",
    val imageUrl: String = "",
    var rating: Float = 0.0f,
    var creatorId: String = "" // NEW: The secret stamp to know who owns this profile!
)