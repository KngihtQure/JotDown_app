package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.*
import com.google.android.material.textfield.TextInputEditText

class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()

        val txtName = findViewById<TextInputEditText>(R.id.username)
        val txtEmail = findViewById<TextInputEditText>(R.id.email)
        val txtPassword = findViewById<TextInputEditText>(R.id.password)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener{
            val username = txtName.text.toString()
            val email = txtEmail.text.toString()
            val password = txtPassword.text.toString()

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        val uid = auth.currentUser!!.uid
                        val user = User(
                            username,
                            email
                        )
                        FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .setValue(user)

                        Toast.makeText(this, "Register Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Login::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}