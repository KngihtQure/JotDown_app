package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.*
import com.google.android.material.textfield.TextInputEditText

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val txtEmail = findViewById<TextInputEditText>(R.id.email)
        val txtPassword = findViewById<TextInputEditText>(R.id.password)

        val btnregister = findViewById<Button>(R.id.btnRegister)
        val btnlogin = findViewById<Button>(R.id.btnLogin)

        btnlogin.setOnClickListener {
            val email = txtEmail.text.toString()
            val password = txtPassword.text.toString()

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(
                email,
                password
            )
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Invalid Login", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnregister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }
    }
}