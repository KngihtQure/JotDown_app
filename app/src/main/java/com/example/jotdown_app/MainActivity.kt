package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val txtHi = findViewById<TextView>(R.id.tvGreeting)
        val txtemail = findViewById<TextView>(R.id.tvEmail)
        val createNote = findViewById<Button>(R.id.btnCreateNote)

        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener {
                val username = it.child("username").value.toString()
                val email = it.child("email").value.toString()

                txtHi.text = "Hi, $username."
                txtemail.text = "$email"
            }

        createNote.setOnClickListener {

        }
    }
}