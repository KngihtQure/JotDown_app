package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Create_note : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_note)

        val etTitle = findViewById<EditText>(R.id.etNoteTitle)
        val etContent = findViewById<EditText>(R.id.etNoteContent)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        val txtHi = findViewById<TextView>(R.id.tvGreeting)
        val txtEmail = findViewById<TextView>(R.id.tvEmail)
        val layoutAddImage = findViewById<LinearLayout>(R.id.layoutAddImage)


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        val uid = currentUser.uid
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value.toString()
                    val email = snapshot.child("email").value.toString()

                    txtHi.text = "Hi, $username."
                    txtEmail.text = email
                }
            }


        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


        btnCancel.setOnClickListener {
            finish()
        }


        layoutAddImage.setOnClickListener {
            Toast.makeText(this, "Image selection coming soon!", Toast.LENGTH_SHORT).show()
        }


        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (content.isEmpty()) {
                etContent.error = "Body is required"
                etContent.requestFocus()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance().getReference("notes").child(uid)
            val noteId = database.push().key ?: return@setOnClickListener

            val noteData = mapOf(
                "id" to noteId,
                "title" to title,
                "content" to content,
                "timestamp" to System.currentTimeMillis()
            )

            database.child(noteId).setValue(noteData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save note: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}