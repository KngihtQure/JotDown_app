package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.jotdown_app.databinding.ActivityCreateFolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class Create_Folder: AppCompatActivity() {
    private lateinit var binding: ActivityCreateFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        val uid = currentUser.uid
        FirebaseDatabase.getInstance().getReference("users").child(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.child("username").value.toString().replaceFirstChar { it.titlecase() }
                    val email = document.child("email").value.toString()
                    binding.tvGreeting.text = "Hi, ${username}."
                    binding.tvEmail.text = email
                }
            }

        binding.btnSave.setOnClickListener {
            val title = binding.etFolderTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etFolderTitle.error = "Title is required"
                binding.etFolderTitle.requestFocus()
                return@setOnClickListener
            }
            saveFoldertoDb(uid, title)
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    private fun saveFoldertoDb(uid: String, folderTitle: String){
        val db = FirebaseFirestore.getInstance().collection("folders").document(uid).collection("user_folders")

        val folderId = db.document().id

        val folderData = mapOf(
            "id" to folderId,
            "title" to folderTitle,
            "timestamp" to System.currentTimeMillis()
        )
        db.document(folderId).set(folderData)
            .addOnSuccessListener{
                Toast.makeText(this, "Folder created!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener{ e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}