package com.example.jotdown_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.jotdown_app.databinding.ActivityCreateNoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class Create_note : AppCompatActivity() {

    private lateinit var binding: ActivityCreateNoteBinding
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Image Picker Launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
                binding.layoutAddImage.visibility = View.GONE
            }
        }

        // Fetch User Info
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        val uid = currentUser.uid
        FirebaseDatabase.getInstance().getReference("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value.toString()
                    val email = snapshot.child("email").value.toString()
                    binding.tvGreeting.text = "Hi, $username."
                    binding.tvEmail.text = email
                }
            }

        // Click Listeners
        binding.layoutAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.ivImagePreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etNoteTitle.text.toString().trim()
            val content = binding.etNoteContent.text.toString().trim()

            if (title.isEmpty()) {
                binding.etNoteTitle.error = "Title is required"
                binding.etNoteTitle.requestFocus()
                return@setOnClickListener
            }

            if (content.isEmpty()) {
                binding.etNoteContent.error = "Body is required"
                binding.etNoteContent.requestFocus()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadImageAndSaveNote(uid, title, content, selectedImageUri!!)
            } else {
                saveNoteToDatabase(uid, title, content, "")
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun uploadImageAndSaveNote(uid: String, title: String, content: String, imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes_images/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                saveNoteToDatabase(uid, title, content, downloadUri.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveNoteToDatabase(uid: String, title: String, content: String, imageUrl: String) {
        val database = FirebaseDatabase.getInstance().getReference("notes").child(uid)
        val noteId = database.push().key ?: return

        val noteData = mapOf(
            "id" to noteId,
            "title" to title,
            "content" to content,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        database.child(noteId).setValue(noteData)
            .addOnSuccessListener {
                Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save note: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
